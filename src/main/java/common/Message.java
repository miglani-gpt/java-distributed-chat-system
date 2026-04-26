package common;

import java.util.Objects;

public class Message {

    private MessageType type;
    private String sender;
    private String receiver;
    private String content;
    private String command;

    // ======================
    // Constructor
    // ======================
    public Message(MessageType type, String sender, String receiver, String content, String command) {
        this.type = Objects.requireNonNull(type, "Message type cannot be null");
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.command = command;
    }

    public Message() {}

    // ======================
    // Getters
    // ======================
    public MessageType getType() { return type; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getContent() { return content; }
    public String getCommand() { return command; }

    // ======================
    // Serialize → JSON
    // ======================
    public String toJson() {
        StringBuilder sb = new StringBuilder("{");

        append(sb, "type", type != null ? type.name() : null);
        append(sb, "sender", sender);
        append(sb, "receiver", receiver);
        append(sb, "content", content);
        append(sb, "command", command);

        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }

        sb.append("}");
        return sb.toString();
    }

    private void append(StringBuilder sb, String key, String value) {
        if (value == null) return;
        sb.append("\"").append(key).append("\":\"")
          .append(escape(value))
          .append("\",");
    }

    // ======================
    // Deserialize ← JSON
    // ======================
    public static Message fromJson(String json) {
        if (json == null || json.isBlank()) return null;

        try {
            Message msg = new Message();

            String typeStr = extract(json, "type");
            if (typeStr == null) return null;

            msg.type = MessageType.fromString(typeStr); // 🔥 FIXED
            if (msg.type == null) return null;

            msg.sender = extract(json, "sender");
            msg.receiver = extract(json, "receiver");
            msg.content = extract(json, "content");
            msg.command = extract(json, "command");

            return msg;

        } catch (Exception e) {
            return null;
        }
    }

    private static String extract(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);

        if (start == -1) return null;
        start += pattern.length();

        StringBuilder value = new StringBuilder();
        boolean escaping = false;

        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaping) {
                value.append(unescapeChar(c));
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                break;
            } else {
                value.append(c);
            }
        }

        return value.toString();
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static char unescapeChar(char c) {
        switch (c) {
            case 'n': return '\n';
            case 'r': return '\r';
            case 't': return '\t';
            case '\\': return '\\';
            case '"': return '"';
            default: return c;
        }
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", content='" + content + '\'' +
                ", command='" + command + '\'' +
                '}';
    }
}