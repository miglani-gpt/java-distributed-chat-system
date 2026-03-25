package common;

public class MessageValidator {

    public static boolean isValid(Message msg) {

        if (msg == null || msg.getType() == null) return false;

        switch (msg.getType()) {

            case CHAT:
                return notEmpty(msg.getSender()) &&
                       notEmpty(msg.getContent());

            case PRIVATE:
                return notEmpty(msg.getSender()) &&
                       notEmpty(msg.getReceiver()) &&
                       notEmpty(msg.getContent());

            case COMMAND:
                return validateCommand(msg);

            case SYSTEM:
            case ERROR:
                return true; // 🔥 relaxed (important)

            case PING:
            case PONG:
                return true; // ❤️ heartbeat allowed

            default:
                return false;
        }
    }

    // ==============================
    // Command-specific validation
    // ==============================
    private static boolean validateCommand(Message msg) {

        String command = msg.getCommand();

        if (!notEmpty(msg.getSender()) || !notEmpty(command)) return false;

        switch (command) {

            case "LIST":
            case "EXIT":
                return true;

            case "NAME":
                return notEmpty(msg.getContent());

            case "PRIVATE":
                return notEmpty(msg.getReceiver()) &&
                       notEmpty(msg.getContent());

            default:
                return false;
        }
    }

    // ==============================
    // Utility
    // ==============================
    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }
}