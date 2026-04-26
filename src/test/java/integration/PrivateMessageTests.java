package integration;

import common.MessageFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PrivateMessageTests extends BaseTest {

    @Test
    void testPrivateMessageDeliveredOnlyToReceiver() throws Exception {
        try (TestClient alice = new TestClient("Alice");
             TestClient bob = new TestClient("Bob");
             TestClient charlie = new TestClient("Charlie")) {

            alice.drain(400);
            bob.drain(400);
            charlie.drain(400);

            alice.send(MessageFactory.privateMsg("Alice", "Bob", "secret-message"));

            assertNotNull(alice.waitForContains("secret-message", 2000));
            assertNotNull(bob.waitForContains("secret-message", 2000));
            assertNull(charlie.waitForContains("secret-message", 1200));
        }
    }
}