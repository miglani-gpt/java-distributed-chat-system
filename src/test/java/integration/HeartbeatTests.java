package integration;

import common.MessageFactory;
import common.MessageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HeartbeatTests extends BaseTest {

    @Test
    void testPingGetsPong() throws Exception {
        try (TestClient alice = new TestClient("Alice")) {
            alice.drain(400);

            alice.send(MessageFactory.ping("Alice"));

            assertNotNull(alice.waitForType(MessageType.PONG, 2000));
        }
    }
}