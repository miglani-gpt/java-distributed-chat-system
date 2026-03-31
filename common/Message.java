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
    // Serialize → JSON (IMPROVED)
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
    // Deserialize ← JSON (IMPROVED)
    // ======================
    public static Message fromJson(String json) {
        if (json == null || json.isEmpty()) return null;

        Message msg = new Message();

        try {
            String typeStr = extract(json, "type");
            if (typeStr == null || typeStr.isEmpty()) return null;

            msg.setType(MessageType.valueOf(typeStr));
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
    // Safe extractor (ESCAPE-AWARE)
    // ======================
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

    // ======================
    // Escape handling
    // ======================
    private String safe(Object value) {
        if (value == null) return "";

        return value.toString()
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
}