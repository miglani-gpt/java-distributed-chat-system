package common;

import common.Message;
import common.MessageType;

public class TestMessage {
    public static void main(String[] args) {

        Message msg = new Message(
            MessageType.CHAT,
            "John",
            "Hello World"
        );

        String json = msg.toJson();
        System.out.println("JSON: " + json);

        Message parsed = Message.fromJson(json);

        System.out.println("Parsed Type: " + parsed.getType());
        System.out.println("Parsed Sender: " + parsed.getSender());
        System.out.println("Parsed Content: " + parsed.getContent());
    }
}