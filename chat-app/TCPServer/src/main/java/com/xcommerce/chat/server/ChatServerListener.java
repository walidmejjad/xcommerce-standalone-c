package com.xcommerce.chat.server;

import java.util.List;

public interface ChatServerListener {
    void onLog(String message);

    void onUsersUpdated(List<String> usernames);
}
