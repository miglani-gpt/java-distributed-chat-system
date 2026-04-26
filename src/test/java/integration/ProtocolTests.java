package integration;

import common.Message;
import common.MessageFactory;
import common.MessageType;
import common.MessageValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProtocolTests {

    @Test
    void testMessageTypeParsing() {
        assertEquals(MessageType.CHAT, MessageType.fromString("chat"));
        assertEquals(MessageType.PRIVATE, MessageType.fromString("PRIVATE"));
        assertEquals(MessageType.PING, MessageType.fromString("Ping"));
        assertNull(MessageType.fromString("unknown"));
    }

    @Test
    void testMessageRoundTripWithEscapes() {
        String original = "Hello \"Bob\"\nLine2\tTabbed\\Path";

        Message msg = new Message(
                MessageType.CHAT,
                "Alice",
                null,
                original,
                null
        );

        String json = msg.toJson();
        Message parsed = Message.fromJson(json);

        assertNotNull(parsed);
        assertEquals(MessageType.CHAT, parsed.getType());
        assertEquals("Alice", parsed.getSender());
        assertEquals(original, parsed.getContent());
    }

    @Test
    void testValidatorAcceptsValidMessages() {
        assertTrue(MessageValidator.isValid(MessageFactory.chat("Alice", "hello")));
        assertTrue(MessageValidator.isValid(MessageFactory.privateMsg("Alice", "Bob", "secret")));
        assertTrue(MessageValidator.isValid(MessageFactory.command("Alice", "LIST", null, "")));
        assertTrue(MessageValidator.isValid(MessageFactory.ping("Alice")));
        assertTrue(MessageValidator.isValid(MessageFactory.pong()));
    }

    @Test
    void testValidatorRejectsBadMessages() {
        assertFalse(MessageValidator.isValid(null));
        assertFalse(MessageValidator.isValid(new Message()));
        assertFalse(MessageValidator.isValid(MessageFactory.command("Alice", "BOGUS", null, "")));
        assertFalse(MessageValidator.isValid(new Message(MessageType.CHAT, null, null, "hello", null)));
    }

    @Test
    void testRoomEventFactoryCreatesSystemMessage() {
        Message msg = MessageFactory.roomEvent("room1", "joined");
        assertNotNull(msg);
        assertEquals(MessageType.SYSTEM, msg.getType());
        assertTrue(msg.getContent().contains("[ROOM room1]"));
    }
}