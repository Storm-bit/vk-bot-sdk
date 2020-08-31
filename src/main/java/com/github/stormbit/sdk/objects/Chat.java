package com.github.stormbit.sdk.objects;

import com.github.stormbit.sdk.callbacks.Callback;
import com.github.stormbit.sdk.clients.Client;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by PeterSamokhin on 29/09/2017 02:49
 * Updated by Stormbit on 10/04/2020 16:37
 */
@SuppressWarnings({"unused"})
public class Chat {

    public static final Integer CHAT_PREFIX = 2000000000;

    private final Client client;
    private final Integer chatId;

    public Chat(Client client, Integer chatId) {
        this.client = client;
        this.chatId = chatId;
    }

    public void addUser(Integer userId, Callback<Object>... callbacks) {
        this.client.api().call("messages.addChatUser", "{chat_id:" + (chatId - CHAT_PREFIX) + ",user_id:" + userId + "}", response -> {
            if (callbacks.length > 0) {
                callbacks[0].onResult(response);
            }
        });
    }

    public void kickUser(Integer userId, Callback<Object>... callbacks) {
        this.client.api().call("messages.removeChatUser", "{chat_id:" + (chatId - CHAT_PREFIX) + ",user_id:\"" + userId + "\"}", response -> {
            if (callbacks.length > 0) {
                callbacks[0].onResult(response);
            }
        });
    }

    public void deletePhoto(Callback<Object>... callbacks) {
        this.client.api().call("messages.deleteChatPhoto", "{chat_id:" + (chatId - CHAT_PREFIX) + "}", response -> {
            if (callbacks.length > 0) {
                callbacks[0].onResult(response);
            }
        });
    }

    public void editTitle(String newTitle, Callback<Object>... callbacks) {
        this.client.api().call("messages.editChat", "{chat_id:" + (chatId - CHAT_PREFIX) + ",title:" + newTitle + "}", response -> {
            if (callbacks.length > 0) {
                callbacks[0].onResult(response);
            }
        });
    }

    public void getUsers(String fields, Callback<JSONArray> callback) {
        this.client.api().call("messages.getChatUsers", "{chat_id:" + (chatId - CHAT_PREFIX) + ",fields:" + fields + "}", response -> {
            callback.onResult(new JSONArray(response.toString()));
        });
    }

    public void getChatInfo(Callback<JSONObject> callback) {

        client.api().call("messages.getChat", "{chat_id:" + chatId + "}", response ->
                callback.onResult((JSONObject) response)
        );
    }
}
