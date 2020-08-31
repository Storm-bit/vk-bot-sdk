package com.github.stormbit.sdk.longpoll;

import com.github.stormbit.sdk.callbacks.AbstractCallback;
import com.github.stormbit.sdk.callbacks.Callback;
import com.github.stormbit.sdk.clients.Client;
import org.json.JSONArray;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.github.stormbit.sdk.clients.Client.scheduler;

/**
 * Created by Storm-bit on 03/04/2020 19:40
 */
public abstract class UpdatesHandler extends Thread {
    protected volatile Queue queue = new Queue();

    protected volatile boolean sendTyping = false;

    /**
     * Maps with callbacks
     */
    protected ConcurrentHashMap<String, Callback> callbacks = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<String, AbstractCallback> chatCallbacks = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<String, AbstractCallback> abstractCallbacks = new ConcurrentHashMap<>();

    /**
     * Client with access_token
     */
    protected Client client;

    public UpdatesHandler(Client client) {
        this.client = client;
    }

    /**
     * Handle the array of updates
     */
    void handle(JSONArray updates) {
        this.queue.putAll(updates);
    }

    @Override
    public void run() {
        scheduler.scheduleWithFixedDelay(this::handleCurrentUpdate, 0, 1, TimeUnit.MILLISECONDS);
    }

    /**
     * Handle one event from longpoll server
     */
    protected abstract void handleCurrentUpdate();

    /**
     * Add callback to the map
     *
     * @param name     Callback name
     * @param callback Callback
     */
    void registerCallback(String name, Callback<Object> callback) {
        this.callbacks.put(name, callback);
    }

    void registerAbstractCallback(String name, AbstractCallback callback) {
        this.abstractCallbacks.put(name, callback);
    }

    void registerChatCallback(String name, AbstractCallback callback) {
        this.chatCallbacks.put(name, callback);
    }

    /**
     * Returns count of callbacks
     */
    int callbacksCount() {
        return this.callbacks.size();
    }

    /**
     * Returns count of abstract callbacks
     */
    int abstractCallbacksCount() {
        return this.abstractCallbacks.size();
    }

    /**
     * Returns count of chat callbacks
     */
    int chatCallbacksCount() {
        return this.chatCallbacks.size();
    }

    /**
     * Returns count of commands
     */
    int commandsCount() {
        return this.client.commands.size();
    }
}
