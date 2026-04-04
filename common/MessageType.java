package common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum MessageType {

    CHAT,
    PRIVATE,
    SYSTEM,
    COMMAND,
    ERROR,

    PING,
    PONG;

    // Faster + thread-safe
    private static final Map<String, MessageType> LOOKUP = new ConcurrentHashMap<>();

    static {
        for (MessageType type : values()) {
            LOOKUP.put(type.name(), type);
        }
    }

    public static MessageType fromString(String value) {
        if (value == null) return null;
        return LOOKUP.get(value.toUpperCase());
    }

    public boolean isUserMessage() {
        return this == CHAT || this == PRIVATE;
    }

    public boolean isSystemMessage() {
        return this == SYSTEM || this == ERROR;
    }

    public boolean isHeartbeat() {
        return this == PING || this == PONG;
    }

    public boolean isCommand() {
        return this == COMMAND;
    }
}