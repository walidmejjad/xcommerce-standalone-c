package com.xcommerce.chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private PrintWriter writer;
    private String username;
    private boolean readOnly;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            writer = new PrintWriter(socket.getOutputStream(), true);
            String intro = reader.readLine();
            if (intro != null && intro.startsWith("USER")) {
                String requested = intro.length() > 4 ? intro.substring(4).trim() : "";
                readOnly = requested.isBlank();
                username = server.registerClient(this, requested, readOnly);
            } else {
                readOnly = true;
                username = server.registerClient(this, "", true);
            }
            send("SYSTEM Connected as %s".formatted(username));

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.equalsIgnoreCase("bye") || trimmed.equalsIgnoreCase("end")) {
                    send("SYSTEM Disconnecting...");
                    break;
                }
                if (trimmed.equalsIgnoreCase("allUsers")) {
                    server.sendUserList(this);
                    continue;
                }
                if (server.isReadOnly(username)) {
                    send("SYSTEM Read-only mode enabled. Messages are not sent.");
                    continue;
                }
                server.broadcastMessage(username, line, this);
            }
        } catch (IOException e) {
            server.log("Connection error: %s".formatted(e.getMessage()));
        } finally {
            close();
            server.unregisterClient(username);
        }
    }

    public void send(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
