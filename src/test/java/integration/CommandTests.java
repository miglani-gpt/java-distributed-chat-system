package integration;

import common.MessageFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CommandTests extends BaseTest {

    @Test
    void testListCommandShowsOnlineUsers() throws Exception {
        try (TestClient alice = new TestClient("Alice");
             TestClient bob = new TestClient("Bob")) {

            alice.drain(400);
            bob.drain(400);

            alice.send(MessageFactory.command("Alice", "LIST", null, ""));
            String response = alice.waitForContains("Online Users:", 2000);

            assertNotNull(response);
            assertTrue(response.contains("Alice"));
            assertTrue(response.contains("Bob"));
        }
    }

    @Test
    void testNameCommandBroadcastsRename() throws Exception {
        try (TestClient alice = new TestClient("Alice");
             TestClient bob = new TestClient("Bob")) {

            alice.drain(400);
            bob.drain(400);

            alice.send(MessageFactory.command("Alice", "NAME", null, "Alicia"));

            String renameNotice = bob.waitForContains("is now known as Alicia", 2000);
            assertNotNull(renameNotice);

            bob.send(MessageFactory.command("Bob", "LIST", null, ""));
            String listResponse = bob.waitForContains("Online Users:", 2000);

            assertNotNull(listResponse);
            assertTrue(listResponse.contains("Alicia"));
        }
    }

    @Test
    void testExitCommandDisconnectsClient() throws Exception {
        try (TestClient alice = new TestClient("Alice")) {
            alice.drain(400);

            alice.send(MessageFactory.command("Alice", "EXIT", null, ""));

            // Wait for the server’s exit acknowledgment if you send one
            alice.waitForContains("Goodbye!", 2000);

            // Then verify the connection actually closes
            assertTrue(alice.awaitDisconnect(3000));
        }
    }
}