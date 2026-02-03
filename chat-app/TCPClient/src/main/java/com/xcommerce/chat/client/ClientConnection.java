package com.xcommerce.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnection {
    private final String host;
    private final int port;
    private final ClientConnectionListener listener;
    private Socket socket;
    private PrintWriter writer;
    private Thread readThread;

    public ClientConnection(String host, int port, ClientConnectionListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public void connect(String username) throws IOException {
        socket = new Socket(host, port);
        writer = new PrintWriter(socket.getOutputStream(), true);
        writer.println("USER " + (username == null ? "" : username));

        readThread = new Thread(() -> readLoop(), "client-read");
        readThread.setDaemon(true);
        readThread.start();
    }

    private void readLoop() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("USERS ")) {
                    listener.onUserList(line.substring(6));
                } else {
                    listener.onMessage(line);
                }
            }
        } catch (IOException e) {
            listener.onSystem("Connection lost: %s".formatted(e.getMessage()));
        }
    }

    public void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    public void disconnect() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
