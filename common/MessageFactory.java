package common;

public final class MessageFactory {

    private static final String SERVER = "SERVER";

    // ==============================
    // CHAT MESSAGE
    // ==============================
    public static Message chat(String sender, String content) {
        sender = clean(sender);
        content = clean(content);

        if (sender == null || content == null) return null;

        return new Message(
                MessageType.CHAT,
                sender,
                null,
                content,
                null
        );
    }

    // ==============================
    // PRIVATE MESSAGE
    // ==============================
    public static Message privateMsg(String sender, String receiver, String content) {
        sender = clean(sender);
        receiver = clean(receiver);
        content = clean(content);

        if (sender == null || receiver == null || content == null) return null;

        return new Message(
                MessageType.PRIVATE,
                sender,
                receiver,
                content,
                null
        );
    }

    // ==============================
    // COMMAND MESSAGE
    // ==============================
    public static Message command(String sender, String command, String receiver, String content) {
        sender = clean(sender);
        command = normalizeCommand(command);
        receiver = clean(receiver);
        content = clean(content);

        if (sender == null || command == null) return null;

        return new Message(
                MessageType.COMMAND,
                sender,
                receiver,
                content,
                command
        );
    }

    // ==============================
    // ❤️ HEARTBEAT MESSAGES
    // ==============================
    public static Message ping(String sender) {
        sender = clean(sender);
        if (sender == null) return null;

        return new Message(
                MessageType.PING,
                sender,
                null,
                null,
                null
        );
    }

    public static Message pong() {
        return new Message(
                MessageType.PONG,
                SERVER,
                null,
                null,
                null
        );
    }

    // ==============================
    // SYSTEM MESSAGE
    // ==============================
    public static Message system(String content) {
        content = clean(content);
        if (content == null) return null;

        return new Message(
                MessageType.SYSTEM,
                SERVER,
                null,
                content,
                null
        );
    }

    // ==============================
    // ROOM EVENT
    // ==============================
    public static Message roomEvent(String room, String content) {
    room = clean(room);
    content = clean(content);

    if (room == null || content == null) return null;

    return system("[ROOM " + room + "] " + content);
}

    // ==============================
    // ERROR MESSAGE
    // ==============================
    public static Message error(String content) {
        content = clean(content);
        if (content == null) return null;

        return new Message(
                MessageType.ERROR,
                SERVER,
                null,
                content,
                null
        );
    }

    // ==============================
    // UTILITIES
    // ==============================
    private static String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeCommand(String command) {
        if (command == null) return null;
        String trimmed = command.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }
}