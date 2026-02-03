package com.xcommerce.chat.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TCPServer extends Application implements ChatServerListener {
    private final Map<String, Color> userColors = new HashMap<>();
    private final Random random = new Random();
    private ListView<String> usersList;
    private TextArea logArea;
    private ChatServer chatServer;

    @Override
    public void start(Stage stage) {
        ServerConfig config = ServerConfig.load();
        GridPane root = new GridPane();
        root.setPadding(new Insets(16));
        root.setHgap(16);
        root.setVgap(12);

        Label statusLabel = new Label("Server Status");
        Circle statusIndicator = new Circle(6, Color.LIMEGREEN);

        usersList = new ListView<>();
        usersList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Color color = userColors.computeIfAbsent(item, key -> Color.hsb(random.nextDouble() * 360, 0.4, 0.9));
                    setStyle("-fx-control-inner-background: " + toRgb(color) + ";");
                }
            }
        });

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);

        root.add(statusLabel, 0, 0);
        root.add(statusIndicator, 1, 0);
        root.add(new Label("Active Users"), 0, 1, 2, 1);
        root.add(usersList, 0, 2, 2, 1);
        root.add(new Label("Server Log"), 0, 3, 2, 1);
        root.add(logArea, 0, 4, 2, 1);

        Scene scene = new Scene(root, 520, 520);
        scene.getStylesheets().add(getClass().getResource("/server.css").toExternalForm());
        stage.setTitle("TCP Server");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> stopServer());
        stage.show();

        chatServer = new ChatServer(config.host(), config.port(), this);
        chatServer.start();
    }

    private String toRgb(Color color) {
        return String.format("rgba(%d, %d, %d, 0.4)",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }

    @Override
    public void stop() {
        stopServer();
    }

    private void stopServer() {
        if (chatServer != null) {
            chatServer.stop();
        }
        Platform.exit();
    }

    @Override
    public void onLog(String message) {
        Platform.runLater(() -> logArea.appendText(message + System.lineSeparator()));
    }

    @Override
    public void onUsersUpdated(List<String> usernames) {
        Platform.runLater(() -> {
            usersList.getItems().setAll(usernames);
            usernames.forEach(name -> userColors.computeIfAbsent(name, key -> Color.hsb(random.nextDouble() * 360, 0.4, 0.9)));
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
