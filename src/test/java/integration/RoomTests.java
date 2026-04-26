package integration;

import common.MessageFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RoomTests extends BaseTest {

    @Test
    void testJoinRoomIsolation() throws Exception {
        try (TestClient alice = new TestClient("Alice");
             TestClient bob = new TestClient("Bob")) {

            alice.drain(400);
            bob.drain(400);

            alice.send(MessageFactory.command("Alice", "JOIN", null, "room1"));
            assertNotNull(alice.waitForContains("Joined room: room1", 2000));

            alice.send(MessageFactory.chat("Alice", "room1-only"));

            assertNotNull(alice.waitForContains("room1-only", 2000));
            assertNull(bob.waitForContains("room1-only", 1200));
        }
    }

    @Test
    void testLeaveReturnsToGlobal() throws Exception {
        try (TestClient alice = new TestClient("Alice");
             TestClient bob = new TestClient("Bob")) {

            alice.drain(400);
            bob.drain(400);

            alice.send(MessageFactory.command("Alice", "JOIN", null, "room2"));
            assertNotNull(alice.waitForContains("Joined room: room2", 2000));

            alice.send(MessageFactory.command("Alice", "LEAVE", null, ""));
            assertNotNull(alice.waitForContains("Returned to global room", 2000));

            bob.send(MessageFactory.chat("Bob", "global-message"));
            assertNotNull(alice.waitForContains("global-message", 2000));
        }
    }

    @Test
    void testRoomsCommandListsRooms() throws Exception {
        try (TestClient alice = new TestClient("Alice");
             TestClient bob = new TestClient("Bob")) {

            alice.drain(400);
            bob.drain(400);

            alice.send(MessageFactory.command("Alice", "JOIN", null, "roomA"));
            bob.send(MessageFactory.command("Bob", "JOIN", null, "roomB"));

            assertNotNull(alice.waitForContains("Joined room: roomA", 2000));
            assertNotNull(bob.waitForContains("Joined room: roomB", 2000));

            alice.send(MessageFactory.command("Alice", "ROOMS", null, ""));
            String response = alice.waitForContains("Rooms:", 2000);

            assertNotNull(response);
            assertTrue(response.contains("global"));
            assertTrue(response.toLowerCase().contains("rooma"));
            assertTrue(response.toLowerCase().contains("roomb"));
        }
    }
}