package client;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.Optional;

public class ChatFX extends Application {

    @Override
    public void start(Stage stage) {

        String username = askUsername();
        if (username == null) {
            System.exit(0);
            return;
        }

        Client client = new Client(username);
        ChatController controller = new ChatController(client);
        ChatView view = new ChatView(controller);

        view.setUsername(username);

        Scene scene = new Scene(view, 720, 580);

        // 🔥 Safe CSS loading
        try {
            scene.getStylesheets().add(
                    getClass().getResource("style.css").toExternalForm()
            );
        } catch (Exception e) {
            System.out.println("⚠ CSS not loaded");
        }

        stage.setTitle("Modern Chat — " + username);
        stage.setScene(scene);

        // 🔥 Better window behavior
        stage.setMinWidth(600);
        stage.setMinHeight(450);
        centerStage(stage);

        stage.show();

        // ✅ Start AFTER UI is visible
        controller.start();

        // 🔥 Graceful shutdown
        stage.setOnCloseRequest(e -> {
            client.shutdown();
            System.exit(0);
        });
    }

    // 🎯 Center window on screen
    private void centerStage(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX((bounds.getWidth() - 720) / 2);
        stage.setY((bounds.getHeight() - 580) / 2);
    }

    // 🧠 Improved username dialog
    private String askUsername() {

        TextInputDialog dialog = new TextInputDialog("User" + (System.currentTimeMillis() % 1000));
        dialog.setTitle("Welcome");
        dialog.setHeaderText("Enter your username");
        dialog.setContentText("Username:");

        Optional<String> result = dialog.showAndWait();

        if (result.isEmpty()) return null;

        String name = result.get().trim();

        if (name.isEmpty()) return null;

        return name;
    }

    public static void main(String[] args) {
        launch();
    }
}