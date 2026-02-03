package com.xcommerce.chat.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ClientConfig {
    private final String host;
    private final int port;

    private ClientConfig(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public static ClientConfig load() {
        Properties properties = new Properties();
        Path externalPath = Path.of("client.properties");
        if (Files.exists(externalPath)) {
            try (InputStream inputStream = Files.newInputStream(externalPath)) {
                properties.load(inputStream);
            } catch (IOException ignored) {
            }
        } else {
            try (InputStream inputStream = ClientConfig.class.getResourceAsStream("/client.properties")) {
                if (inputStream != null) {
                    properties.load(inputStream);
                }
            } catch (IOException ignored) {
            }
        }
        String host = properties.getProperty("server.host", "localhost");
        int port = Integer.parseInt(properties.getProperty("server.port", "3000"));
        return new ClientConfig(host, port);
    }
}
