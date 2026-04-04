package client;

import common.Message;
import common.MessageFactory;

public class ChatController {

    private final Client client;
    private ChatView view;

    public ChatController(Client client) {
        this.client = client;
    }

    public void setView(ChatView view) {
        this.view = view;
    }

    // 🚀 Start connection
    public void start() {
        try {
            client.setMessageListener(this::onMessage);
            client.start();

            // set username in UI
            view.setUsername(client.getUsername());

            // request user list
            client.send("/list");

            updateStatus("🟢 Connected");

        } catch (Exception e) {
            updateStatus("🔴 Connection Failed");
            view.display(MessageFactory.error("Unable to connect to server"));
        }
    }

    // 📤 Send message
    public void send(String msg) {
        if (msg == null || msg.trim().isEmpty()) return;

        try {
            client.send(msg);

            // ❌ REMOVED: no fake "[YOU]" messages
            // Client already emits real Message object

        } catch (Exception e) {
            view.display(MessageFactory.error("Failed to send message"));
        }
    }

    // 📩 Receive message (NOW TYPE-SAFE)
    private void onMessage(Message msg) {
        if (msg == null) return;

        view.display(msg);
    }

    // 🔄 Update status bar (CLEAN)
    private void updateStatus(String status) {
        if (view != null) {
            view.setStatus(status);
        }
    }
}