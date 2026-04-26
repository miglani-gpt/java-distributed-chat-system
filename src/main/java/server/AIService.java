package server;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIService implements AutoCloseable {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration TOXICITY_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration SUMMARIZE_TIMEOUT = Duration.ofSeconds(5);

    private static final int MAX_RETRIES = 2;
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(250);

    private static final Pattern TOXIC_PATTERN = Pattern.compile(
            "\"toxic\"\\s*:\\s*(true|false|1|0)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern SUMMARY_PATTERN = Pattern.compile(
            "\"summary\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private final HttpClient client;
    private final ScheduledExecutorService scheduler;
    private final String baseUrl;

    public AIService() {
        this("http://localhost:8000");
    }

    public AIService(String baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ai-service-retry");
            t.setDaemon(true);
            return t;
        });
    }

    public CompletableFuture<Boolean> isToxicAsync(String text) {
        if (text == null || text.trim().isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        String json = "{\"text\":\"" + escapeJson(text) + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/toxicity"))
                .timeout(TOXICITY_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        long start = System.currentTimeMillis();

        return sendWithRetry(request, MAX_RETRIES, INITIAL_BACKOFF, "toxicity")
                .thenApply(response -> {
                    long elapsed = System.currentTimeMillis() - start;
                    log("TOXICITY", "HTTP " + response.statusCode() + " in " + elapsed + "ms");
                    return parseToxic(response.body());
                })
                .exceptionally(e -> {
                    log("TOXICITY", "Failed: " + rootMessage(e));
                    return false; // fail-safe: allow message if AI is unavailable
                });
    }

    public CompletableFuture<String> summarizeAsync(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return CompletableFuture.completedFuture("No messages to summarize.");
        }

        StringBuilder json = new StringBuilder("{\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            String msg = messages.get(i);
            json.append("\"").append(escapeJson(msg == null ? "" : msg)).append("\"");
            if (i < messages.size() - 1) {
                json.append(",");
            }
        }
        json.append("]}");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/summarize"))
                .timeout(SUMMARIZE_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        long start = System.currentTimeMillis();

        return sendWithRetry(request, MAX_RETRIES, INITIAL_BACKOFF, "summarize")
                .thenApply(response -> {
                    long elapsed = System.currentTimeMillis() - start;
                    log("SUMMARY", "HTTP " + response.statusCode() + " in " + elapsed + "ms");
                    return extractSummary(response.body());
                })
                .exceptionally(e -> {
                    log("SUMMARY", "Failed: " + rootMessage(e));
                    return "⚠️ Summary failed.";
                });
    }

    private CompletableFuture<HttpResponse<String>> sendWithRetry(
            HttpRequest request,
            int retriesLeft,
            Duration backoff,
            String label
    ) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle((response, error) -> {
                    if (error != null) {
                        if (retriesLeft > 0) {
                            log(label, "Request error, retrying: " + rootMessage(error));
                            return delayedRetry(request, retriesLeft - 1, backoff.multipliedBy(2), label);
                        }
                        return CompletableFuture.<HttpResponse<String>>failedFuture(error);
                    }

                    if (shouldRetry(response.statusCode()) && retriesLeft > 0) {
                        log(label, "HTTP " + response.statusCode() + ", retrying...");
                        return delayedRetry(request, retriesLeft - 1, backoff.multipliedBy(2), label);
                    }

                    return CompletableFuture.completedFuture(response);
                })
                .thenCompose(future -> future);
    }

    private CompletableFuture<HttpResponse<String>> delayedRetry(
            HttpRequest request,
            int retriesLeft,
            Duration delay,
            String label
    ) {
        CompletableFuture<Void> wait = new CompletableFuture<>();
        scheduler.schedule(() -> wait.complete(null), delay.toMillis(), TimeUnit.MILLISECONDS);

        return wait.thenCompose(v -> sendWithRetry(request, retriesLeft, delay, label));
    }

    private boolean shouldRetry(int statusCode) {
        return statusCode == 408 || statusCode == 425 || statusCode == 429
                || statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    private boolean parseToxic(String body) {
        if (body == null) return false;

        try {
            Matcher matcher = TOXIC_PATTERN.matcher(body);
            if (!matcher.find()) return false;

            String value = matcher.group(1).toLowerCase();
            return value.equals("true") || value.equals("1");
        } catch (Exception e) {
            log("PARSE", "Toxic parse failed: " + e.getMessage());
            return false;
        }
    }

    private String extractSummary(String body) {
        if (body == null || body.trim().isEmpty()) {
            return "⚠️ Invalid response.";
        }

        try {
            Matcher matcher = SUMMARY_PATTERN.matcher(body);
            if (!matcher.find()) {
                return "⚠️ Invalid summary response.";
            }

            String raw = matcher.group(1);
            return unescapeJsonString(raw);
        } catch (Exception e) {
            log("PARSE", "Summary parse failed: " + e.getMessage());
            return "⚠️ Invalid summary response.";
        }
    }

    private String escapeJson(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    private String unescapeJsonString(String s) {
        StringBuilder out = new StringBuilder();
        boolean escaping = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (escaping) {
                switch (c) {
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case '\\' -> out.append('\\');
                    case '"' -> out.append('"');
                    case '/' -> out.append('/');
                    default -> out.append(c);
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else {
                out.append(c);
            }
        }

        return out.toString();
    }

    private String normalizeBaseUrl(String url) {
        Objects.requireNonNull(url, "baseUrl cannot be null");
        String trimmed = url.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur.getMessage() != null ? cur.getMessage() : cur.getClass().getSimpleName();
    }

    private void log(String tag, String msg) {
        System.out.println("[AI][" + tag + "] " + msg);
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}