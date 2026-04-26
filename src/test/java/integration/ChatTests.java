package integration;

import common.MessageFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ChatTests extends BaseTest {

    @Test
    void testPublicChatBroadcast() throws Exception {

        TestClient alice = new TestClient("Alice");
        TestClient bob = new TestClient("Bob");

        alice.send(MessageFactory.chat("Alice", "hello"));

        String msg = bob.waitForContains("hello", 2000);

        assertNotNull(msg);

        alice.close();
        bob.close();
    }
}