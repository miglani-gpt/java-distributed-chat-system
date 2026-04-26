package integration;

import common.MessageFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RoomAndListTests extends BaseTest {

    @Test
    void testListShowsAllConnectedUsers() throws Exception {
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
    void testRoomsListsCreatedRooms() throws Exception {
        try (TestClient alice = new TestClient("Alice");
             TestClient bob = new TestClient("Bob")) {

            alice.drain(400);
            bob.drain(400);

            alice.send(MessageFactory.command("Alice", "JOIN", null, "alpha"));
            bob.send(MessageFactory.command("Bob", "JOIN", null, "beta"));

            assertNotNull(alice.waitForContains("Joined room: alpha", 2000));
            assertNotNull(bob.waitForContains("Joined room: beta", 2000));

            alice.send(MessageFactory.command("Alice", "ROOMS", null, ""));

            String response = alice.waitForContains("Rooms:", 2000);
            assertNotNull(response);
            assertTrue(response.toLowerCase().contains("alpha"));
            assertTrue(response.toLowerCase().contains("beta"));
        }
    }
}