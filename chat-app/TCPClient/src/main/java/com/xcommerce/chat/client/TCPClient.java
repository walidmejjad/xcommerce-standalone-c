package com.xcommerce.chat.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class TCPClient extends Application implements ClientConnectionListener {
    private TextField usernameField;
    private TextArea chatArea;
    private TextField messageField;
    private Button sendButton;
    private Label modeLabel;
    private Label statusLabel;
    private Circle statusIndicator;
    private ClientConnection connection;
    private boolean readOnly;

    @Override
    public void start(Stage stage) {
        ClientConfig config = ClientConfig.load();

        GridPane root = new GridPane();
        root.setPadding(new Insets(16));
        root.setHgap(12);
        root.setVgap(12);

        usernameField = new TextField();
        usernameField.setPromptText("Enter username");

        Button connectButton = new Button("CONNECT");
        connectButton.setOnAction(event -> connect(config));

        statusLabel = new Label("Offline");
        statusIndicator = new Circle(6, Color.DARKGRAY);
        modeLabel = new Label("Disconnected");

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        messageField = new TextField();
        messageField.setPromptText("Type message or 'allUsers'");
        messageField.setOnAction(event -> sendMessage());

        sendButton = new Button("SEND");
        sendButton.setOnAction(event -> sendMessage());

        root.add(new Label("Username"), 0, 0);
        root.add(usernameField, 1, 0);
        root.add(connectButton, 2, 0);
        root.add(statusLabel, 0, 1);
        root.add(statusIndicator, 1, 1);
        root.add(modeLabel, 2, 1);
        root.add(chatArea, 0, 2, 3, 1);
        root.add(messageField, 0, 3, 2, 1);
        root.add(sendButton, 2, 3);

        Scene scene = new Scene(root, 600, 500);
        scene.getStylesheets().add(getClass().getResource("/client.css").toExternalForm());
        stage.setTitle("TCP Client");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> disconnect());
        stage.show();

        applyDisconnectedState();
        if (!getParameters().getRaw().isEmpty()) {
            connect(config);
        }
    }

    private void connect(ClientConfig config) {
        if (connection != null) {
            return;
        }
        String[] raw = getParameters().getRaw().toArray(String[]::new);
        String host = raw.length >= 1 ? raw[0] : config.host();
        int port = raw.length >= 2 ? Integer.parseInt(raw[1]) : config.port();
        String username = usernameField.getText();
        readOnly = username == null || username.isBlank();
        modeLabel.setText(readOnly ? "READ-ONLY MODE" : "Online");

        connection = new ClientConnection(host, port, this);
        try {
            connection.connect(username);
            statusLabel.setText("Online");
            statusIndicator.setFill(Color.LIMEGREEN);
            applyConnectedState();
            onSystem("Connected to %s:%d".formatted(host, port));
        } catch (Exception e) {
            connection = null;
            onSystem("Failed to connect: %s".formatted(e.getMessage()));
            statusLabel.setText("Offline");
            statusIndicator.setFill(Color.DARKGRAY);
        }
    }

    private void applyConnectedState() {
        messageField.setDisable(readOnly);
        sendButton.setDisable(readOnly);
        usernameField.setDisable(true);
    }

    private void applyDisconnectedState() {
        messageField.setDisable(true);
        sendButton.setDisable(true);
        usernameField.setDisable(false);
    }

    private void sendMessage() {
        if (connection == null || readOnly) {
            return;
        }
        String message = messageField.getText();
        if (message == null || message.isBlank()) {
            return;
        }
        connection.sendMessage(message.trim());
        if (message.equalsIgnoreCase("bye") || message.equalsIgnoreCase("end")) {
            disconnect();
        }
        messageField.clear();
    }

    private void disconnect() {
        if (connection != null) {
            connection.sendMessage("bye");
            connection.disconnect();
            connection = null;
            statusLabel.setText("Offline");
            statusIndicator.setFill(Color.DARKGRAY);
            modeLabel.setText("Disconnected");
            applyDisconnectedState();
        }
    }

    @Override
    public void onMessage(String message) {
        Platform.runLater(() -> chatArea.appendText(message + System.lineSeparator()));
    }

    @Override
    public void onSystem(String message) {
        Platform.runLater(() -> chatArea.appendText("SYSTEM: " + message + System.lineSeparator()));
    }

    @Override
    public void onUserList(String users) {
        Platform.runLater(() -> chatArea.appendText("Active users: " + users + System.lineSeparator()));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
