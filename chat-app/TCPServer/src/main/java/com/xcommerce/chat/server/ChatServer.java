package com.xcommerce.chat.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatServer {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String host;
    private final int port;
    private final ChatServerListener listener;
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private final AtomicInteger guestCounter = new AtomicInteger(1);

    private ServerSocket serverSocket;
    private Thread acceptThread;

    public ChatServer(String host, int port, ChatServerListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public void start() {
        acceptThread = new Thread(this::acceptLoop, "chat-server-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        log("Server Started on %s:%d".formatted(host, port));
    }

    private void acceptLoop() {
        try (ServerSocket server = new ServerSocket()) {
            serverSocket = server;
            server.bind(new InetSocketAddress(host, port));
            log("Waiting for Client...");
            while (!server.isClosed()) {
                Socket socket = server.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                clientPool.submit(handler);
            }
        } catch (IOException e) {
            log("Server stopped: %s".formatted(e.getMessage()));
        }
    }

    public void stop() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        clients.values().forEach(ClientHandler::close);
        clientPool.shutdownNow();
        log("Server stopped.");
    }

    public String registerClient(ClientHandler handler, String requestedName, boolean readOnly) {
        String username = normalizeUsername(requestedName, readOnly);
        clients.put(username, handler);
        log("Welcome %s".formatted(username));
        broadcastSystem("%s joined the chat".formatted(username), handler);
        notifyUserList();
        return username;
    }

    private String normalizeUsername(String requestedName, boolean readOnly) {
        if (requestedName == null || requestedName.isBlank()) {
            return "ReadOnly-%d".formatted(guestCounter.getAndIncrement());
        }
        String base = requestedName.trim();
        String username = base;
        int suffix = 1;
        while (clients.containsKey(username)) {
            username = "%s#%d".formatted(base, suffix++);
        }
        return readOnly ? "ReadOnly-%s".formatted(username) : username;
    }

    public void unregisterClient(String username) {
        if (username == null) {
            return;
        }
        clients.remove(username);
        broadcastSystem("%s left the chat".formatted(username), null);
        notifyUserList();
        log("Disconnected %s".formatted(username));
    }

    public void broadcastMessage(String username, String message, ClientHandler sender) {
        String formatted = "[%s] %s: %s".formatted(LocalTime.now().format(TIME_FORMAT), username, message);
        for (ClientHandler handler : clients.values()) {
            if (handler != sender) {
                handler.send(formatted);
            }
        }
        log("Broadcast from %s: %s".formatted(username, message));
    }

    public void broadcastSystem(String message, ClientHandler sender) {
        String formatted = "[%s] SYSTEM: %s".formatted(LocalTime.now().format(TIME_FORMAT), message);
        for (ClientHandler handler : clients.values()) {
            if (handler != sender) {
                handler.send(formatted);
            }
        }
        log(message);
    }

    public void sendUserList(ClientHandler recipient) {
        List<String> usernames = new ArrayList<>(clients.keySet());
        recipient.send("USERS " + String.join(", ", usernames));
    }

    public void notifyUserList() {
        if (listener != null) {
            listener.onUsersUpdated(new ArrayList<>(clients.keySet()));
        }
    }

    public void log(String message) {
        if (listener != null) {
            listener.onLog(message);
        }
    }

    public boolean isReadOnly(String username) {
        return username != null && username.startsWith("ReadOnly-");
    }
}
