package common;

public class MessageFactory {

    // ==============================
    // CHAT MESSAGE
    // ==============================
    public static Message chat(String sender, String content) {
        return new Message(
                MessageType.CHAT,
                clean(sender),
                null,
                clean(content),
                null
        );
    }

    // ==============================
    // PRIVATE MESSAGE
    // ==============================
    public static Message privateMsg(String sender, String receiver, String content) {
        return new Message(
                MessageType.PRIVATE,
                clean(sender),
                clean(receiver),
                clean(content),
                null
        );
    }

    // ==============================
    // COMMAND MESSAGE
    // ==============================
    public static Message command(String sender, String command, String receiver, String content) {
        return new Message(
                MessageType.COMMAND,
                clean(sender),
                clean(receiver),
                clean(content),
                normalizeCommand(command)
        );
    }

    // ==============================
    // ❤️ HEARTBEAT MESSAGES
    // ==============================
    public static Message ping(String sender) {
        return new Message(
                MessageType.PING,
                clean(sender),   // keep for extensibility (optional)
                null,
                null,
                null
        );
    }

    public static Message pong() {
        return new Message(
                MessageType.PONG,
                "SERVER",
                null,
                null,
                null
        );
    }

    // ==============================
    // SYSTEM MESSAGE
    // ==============================
    public static Message system(String content) {
        return new Message(
                MessageType.SYSTEM,
                "SERVER",
                null,
                clean(content),
                null
        );
    }

    // ==============================
    // ERROR MESSAGE
    // ==============================
    public static Message error(String content) {
        return new Message(
                MessageType.ERROR,
                "SERVER",
                null,
                clean(content),
                null
        );
    }

    // ==============================
    // UTILITIES
    // ==============================
    private static String clean(String value) {
        if (value == null) return null; // 🔥 changed (important)
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeCommand(String command) {
        if (command == null) return null;
        String trimmed = command.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }
}