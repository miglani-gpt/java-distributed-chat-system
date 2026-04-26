package integration;

import common.MessageFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServerEdgeCaseTests extends BaseTest {

    @Test
    void testMalformedJsonReturnsError() throws Exception {
        try (TestClient alice = new TestClient("Alice")) {
            alice.drain(400);

            alice.sendRaw("{not valid json}");

            assertNotNull(alice.waitForContains("Invalid message format", 2000));
        }
    }

    @Test
    void testInvalidJoinCommandReturnsError() throws Exception {
        try (TestClient alice = new TestClient("Alice")) {
            alice.drain(400);

            alice.sendRaw("{\"type\":\"COMMAND\",\"sender\":\"Alice\",\"command\":\"JOIN\",\"content\":\"\"}");

            assertNotNull(alice.waitForContains("Invalid message format", 2000));
        }
    }

    @Test
    void testUnknownPrivateRecipientReturnsError() throws Exception {
        try (TestClient alice = new TestClient("Alice")) {
            alice.drain(400);

            alice.send(MessageFactory.privateMsg("Alice", "GhostUser", "secret"));

            assertNotNull(alice.waitForContains("User not found", 2000));
        }
    }

    @Test
    void testRenameToTakenUsernameReturnsError() throws Exception {
        try (TestClient alice = new TestClient("Alice");
             TestClient bob = new TestClient("Bob")) {

            alice.drain(400);
            bob.drain(400);

            alice.send(MessageFactory.command("Alice", "NAME", null, "Bob"));

            assertNotNull(alice.waitForContains("Username already taken", 2000));
        }
    }

    @Test
    void testPingReturnsPong() throws Exception {
        try (TestClient alice = new TestClient("Alice")) {
            alice.drain(400);

            alice.send(MessageFactory.ping("Alice"));

            assertNotNull(alice.waitForContains("\"type\":\"PONG\"", 2000));
        }
    }
}