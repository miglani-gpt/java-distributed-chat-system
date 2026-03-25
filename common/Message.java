package common;

public class Message {

    private MessageType type;
    private String sender;
    private String receiver;
    private String content;
    private String command;

    // ======================
    // Constructors
    // ======================
    public Message(MessageType type, String sender, String receiver, String content, String command) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.command = command;
    }

    public Message(MessageType type, String sender, String receiver, String content) {
        this(type, sender, receiver, content, null);
    }

    public Message(MessageType type, String sender, String content) {
        this(type, sender, null, content, null);
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
    // Setters
    // ======================
    public void setType(MessageType type) { this.type = type; }
    public void setSender(String sender) { this.sender = sender; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    public void setContent(String content) { this.content = content; }
    public void setCommand(String command) { this.command = command; }

    // ======================
    // Serialize → JSON (FIXED)
    // ======================
    public String toJson() {
        return String.format(
            "{\"type\":\"%s\",\"sender\":\"%s\",\"receiver\":\"%s\",\"content\":\"%s\",\"command\":\"%s\"}",
            safe(type),
            safe(sender),
            safe(receiver),
            safe(content),
            safe(command)
        );
    }

    // ======================
    // Deserialize ← JSON
    // ======================
    public static Message fromJson(String json) {
        if (json == null || json.isEmpty()) return null;

        Message msg = new Message();

        try {
            msg.setType(MessageType.valueOf(extract(json, "type")));
            msg.setSender(extract(json, "sender"));
            msg.setReceiver(extract(json, "receiver"));
            msg.setContent(extract(json, "content"));
            msg.setCommand(extract(json, "command"));
        } catch (Exception e) {
            return null;
        }

        return msg;
    }

    // ======================
    // Safe field extractor
    // ======================
    private static String extract(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);

        if (start == -1) return "";

        start += pattern.length();
        int end = json.indexOf("\"", start);

        if (end == -1) return "";

        return json.substring(start, end);
    }

    // ======================
    // Utility (FIXED escaping)
    // ======================
    private String safe(Object value) {
        if (value == null) return "";

        return value.toString()
                .replace("\\", "\\\\")   // escape backslash
                .replace("\"", "\\\"");  // escape quotes
    }
}