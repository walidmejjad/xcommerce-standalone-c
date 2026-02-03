package com.xcommerce.chat.client;

public interface ClientConnectionListener {
    void onMessage(String message);

    void onSystem(String message);

    void onUserList(String users);
}
