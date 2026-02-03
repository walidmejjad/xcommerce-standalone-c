package com.xcommerce.chat.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ServerConfig {
    private final String host;
    private final int port;

    private ServerConfig(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public static ServerConfig load() {
        Properties properties = new Properties();
        Path externalPath = Path.of("server.properties");
        if (Files.exists(externalPath)) {
            try (InputStream inputStream = Files.newInputStream(externalPath)) {
                properties.load(inputStream);
            } catch (IOException ignored) {
            }
        } else {
            try (InputStream inputStream = ServerConfig.class.getResourceAsStream("/server.properties")) {
                if (inputStream != null) {
                    properties.load(inputStream);
                }
            } catch (IOException ignored) {
            }
        }
        String host = properties.getProperty("server.host", "0.0.0.0");
        int port = Integer.parseInt(properties.getProperty("server.port", "3000"));
        return new ServerConfig(host, port);
    }
}
