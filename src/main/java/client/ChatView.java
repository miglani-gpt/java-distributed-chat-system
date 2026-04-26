package client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import common.Message;
import common.MessageType;

public class ChatView extends BorderPane {

    private VBox chatBox = new VBox(14);
    private TextField input = new TextField();
    private ListView<String> userList = new ListView<String>();
    private Label status = new Label("🔴 Disconnected");
    private Label usersLabel = new Label("Users (0)");

    private ScrollPane scroll;

    private ChatController controller;
    private String myUsername;

    public ChatView(ChatController controller) {
        this.controller = controller;
        controller.setView(this);
        setupUI();
    }

    private void setupUI() {

        chatBox.setPadding(new Insets(15));
        chatBox.setFillWidth(true);

        scroll = new ScrollPane(chatBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1e1e1e;");

        // 🔥 Input improvements
        input.setPromptText("Type a message...");
        input.getStyleClass().add("input-field");

        input.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER:
                    if (!e.isShiftDown()) {
                        send();
                        e.consume();
                    }
                    break;
            }
        });

        Button sendBtn = new Button("Send");
        sendBtn.getStyleClass().add("send-btn");
        sendBtn.setOnAction(e -> send());

        HBox bottom = new HBox(10, input, sendBtn);
        bottom.setPadding(new Insets(10));
        bottom.setAlignment(Pos.CENTER);

        // 🔥 Sidebar
        VBox left = new VBox(10, usersLabel, userList);
        left.getStyleClass().add("sidebar");
        left.setPadding(new Insets(10));
        left.setPrefWidth(170);

        status.getStyleClass().add("status-bar");

        setTop(status);
        setCenter(scroll);
        setLeft(left);
        setBottom(bottom);

        setupUserListStyling();
    }

    private void setupUserListStyling() {
        userList.setCellFactory(list -> new ListCell<String>() {
    @Override
    protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.equals(myUsername) ? item + " (You)" : item);
                }
            }
        });
    }

    private void send() {
        String msg = input.getText().trim();
        if (!msg.isEmpty()) {
            controller.send(msg);
            input.clear();
        }
    }

    public void display(Message msg) {

        Platform.runLater(() -> {

            // ✅ USER LIST
            if (msg.getType() == MessageType.SYSTEM &&
                msg.getContent() != null &&
                msg.getContent().startsWith("Online Users:")) {

                String[] users = msg.getContent()
                        .replace("Online Users:", "")
                        .split(",");

                userList.getItems().setAll(users);
                usersLabel.setText("Users (" + users.length + ")");
                return;
            }

            // ❌ Remove empty placeholder if exists
            if (chatBox.getChildren().size() == 1 &&
                chatBox.getChildren().get(0) instanceof Label) {
                chatBox.getChildren().clear();
            }

            HBox bubble = createBubble(msg);
            chatBox.getChildren().add(bubble);

            // 🔥 Smooth scroll
            Platform.runLater(() -> scroll.setVvalue(1.0));
        });
    }

    private HBox createBubble(Message msg) {

        String content = msg.getContent();
        String sender = msg.getSender();

        String time = java.time.LocalTime.now()
                .withNano(0)
                .toString();

        Label text = new Label();
        text.setWrapText(true);
        text.setMaxWidth(360);
        text.getStyleClass().add("message-text");

        HBox bubble = new HBox(text);
        bubble.setPadding(new Insets(6, 10, 6, 10));
        bubble.setOpacity(0);
        javafx.animation.FadeTransition ft =
    new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), bubble);
ft.setFromValue(0);
ft.setToValue(1);
ft.play();

        switch (msg.getType()) {

            case SYSTEM:
                text.setText(content);
                bubble.setAlignment(Pos.CENTER);
                text.getStyleClass().add("system-msg");
                break;

            case ERROR:
                text.setText("❌ " + content);
                bubble.setAlignment(Pos.CENTER);
                text.getStyleClass().add("system-msg");
                break;

            case PRIVATE:
                text.setText("🔒 " + sender + "\n" + content + "   [" + time + "]");
                bubble.setAlignment(Pos.CENTER_LEFT);
                text.getStyleClass().add("other-msg");
                break;

            case CHAT:
                if (sender != null && sender.equals(myUsername)) {
                    text.setText(content + "   [" + time + "]");
                    bubble.setAlignment(Pos.CENTER_RIGHT);
                    text.getStyleClass().add("my-msg");
                } else {
                    text.setText(sender + "\n" + content + "   [" + time + "]");
                    bubble.setAlignment(Pos.CENTER_LEFT);
                    text.getStyleClass().add("other-msg");
                }
                break;

            default:
                text.setText(content);
                bubble.setAlignment(Pos.CENTER_LEFT);
        }

        return bubble;
    }

    public void setUsername(String username) {
        this.myUsername = username;
    }

    public void setStatus(String text) {
        status.setText(text);
    }
}