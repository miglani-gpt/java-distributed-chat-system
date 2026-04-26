package common;

import java.util.Set;

public final class MessageValidator {

    // ==============================
    // Command Registry
    // ==============================
    private static final Set<String> VALID_COMMANDS = Set.of(
            "LIST",
            "EXIT",
            "NAME",
            "JOIN",
            "LEAVE",
            "ROOMS",
            "SUMMARIZE"
    );

    // ==============================
    // Entry Point
    // ==============================
    public static boolean isValid(Message msg) {

        if (msg == null) return false;

        MessageType type = msg.getType();
        if (type == null) return false;

        switch (type) {

            case CHAT:
                return require(msg.getSender(), msg.getContent());

            case PRIVATE:
                return require(msg.getSender(), msg.getReceiver(), msg.getContent());

            case COMMAND:
                return validateCommand(msg);

            case SYSTEM:
            case ERROR:
                // Optional: enforce at least content
                return msg.getContent() != null;

            case PING:
                return notEmpty(msg.getSender());

            case PONG:
                return true; // server-controlled

            default:
                return false;
        }
    }

    // ==============================
    // Command Validation
    // ==============================
    private static boolean validateCommand(Message msg) {

        String sender = msg.getSender();
        String command = normalize(msg.getCommand());

        if (!notEmpty(sender) || command == null) return false;

        if (!VALID_COMMANDS.contains(command)) return false;

        switch (command) {

            case "LIST":
            case "EXIT":
            case "LEAVE":
            case "ROOMS":
                return true;

            case "NAME":
                return notEmpty(msg.getContent());

            case "JOIN":
                return notEmpty(msg.getContent()); // room name

            case "SUMMARIZE":
                return notEmpty(msg.getContent()); // expects number

            default:
                return false;
        }
    }

    // ==============================
    // Utility Helpers
    // ==============================
    private static boolean require(String... fields) {
        for (String f : fields) {
            if (!notEmpty(f)) return false;
        }
        return true;
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t.toUpperCase();
    }
}