package common;

import java.util.Set;

public class MessageValidator {

    // ✅ Centralized command registry (EXTENSIBLE)
    private static final Set<String> VALID_COMMANDS = Set.of(
            "LIST",
            "EXIT",
            "NAME",
            "JOIN",     // ✅ NEW
            "LEAVE",    // ✅ NEW
            "ROOMS"     // ✅ NEW
    );

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
                return true; // relaxed (intentional)

            case PING:
            case PONG:
                return true; // heartbeat always allowed

            default:
                return false;
        }
    }

    // ==============================
    // Command Validation (UPGRADED)
    // ==============================
    private static boolean validateCommand(Message msg) {

        String sender = msg.getSender();
        String command = msg.getCommand();

        if (!notEmpty(sender) || !notEmpty(command)) return false;

        command = command.trim().toUpperCase();

        if (!VALID_COMMANDS.contains(command)) {
            return false;
        }

        switch (command) {

            case "LIST":
            case "EXIT":
            case "LEAVE":
            case "ROOMS":
                // ✅ No content required
                return true;

            case "NAME":
                return notEmpty(msg.getContent());

            case "JOIN":
                return notEmpty(msg.getContent()); // room name required

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