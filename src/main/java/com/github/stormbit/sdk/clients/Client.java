package com.github.stormbit.sdk.clients;

import com.github.stormbit.sdk.callbacks.Callback;
import com.github.stormbit.sdk.callbacks.CallbackDouble;
import com.github.stormbit.sdk.callbacks.CallbackFourth;
import com.github.stormbit.sdk.callbacks.CallbackTriple;
import com.github.stormbit.sdk.longpoll.LongPoll;
import com.github.stormbit.sdk.objects.Chat;
import com.github.stormbit.sdk.objects.Message;
import com.github.stormbit.sdk.utils.Utils;
import com.github.stormbit.sdk.utils.vkapi.API;
import com.github.stormbit.sdk.utils.vkapi.apis.API1;
import com.github.stormbit.sdk.utils.vkapi.Auth;
import com.github.stormbit.sdk.utils.vkapi.apis.API2;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.*;

/**
 * Created by PeterSamokhin on 28/09/2017 21:59
 * Updated by Storm-bit on 03/04/2020 19:40
 *
 * Main client class, that contains all necessary methods and fields
 * for base work with VK and longpoll server
 */
@SuppressWarnings("unused")
public abstract class Client {

    /*
     * Executor services for threadsafing and fast work
     */
    public static final ExecutorService service = Executors.newCachedThreadPool();
    public static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /*
     * Main params
     */
    private Integer id;
    private static API api;
    private LongPoll longPoll;
    private final Auth _auth;
    private String access_token;
    public String token;

    public CopyOnWriteArrayList<Command> commands = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Integer, Chat> chats = new ConcurrentHashMap<>();

    /**
     * @param login        Login of your VK bot account
     * @param password     Password of your VK bot account
     */
    Client(String login, String password) {
        _auth = new Auth(login, password).auth();
        api = new API1(this);
        this.id = Utils.getId(this);

        this.longPoll = new LongPoll(this);
    }

    Client(String access_token, Integer id) {
        _auth = new Auth();
        token = access_token;
        this.id = id;
        api = new API2(this);

        this.longPoll = new LongPoll(this);
    }

    public void setLongPoll(LongPoll LP) {

        this.longPoll.off();
        this.longPoll = LP;
    }

    public Chat chat(Integer chatId) {

        if (!this.chats.containsKey(chatId)) {
            this.chats.put(chatId, new Chat(this, chatId));
        }

        return this.chats.get(chatId);
    }

    /**
     * Get longpoll of current client
     * @return longpoll object
     */
    public LongPoll longPoll() {
        return longPoll;
    }

    public Auth auth() {
        return _auth;
    }

    /**
     * Get API for making requests
     * @return api object
     */
    public API api() {
        return api;
    }

    /**
     * If the client need to start typing
     * after receiving message
     * and until client's message is sent
     * @param enable true or false
     */
    public void enableTyping(boolean enable) {
        this.longPoll().enableTyping(enable);
    }

    /* On every event */

    public void onLongPollEvent(Callback<JSONArray> callback) {
        this.longPoll().registerCallback("OnEveryLongPollEventCallback", callback);
    }

    /* Chats */
    public void onChatJoin(CallbackTriple<Integer, Integer, Integer> callback) {
        this.longPoll().registerChatCallback("OnChatJoinCallback", callback);
    }

    public void onChatLeave(CallbackTriple<Integer, Integer, Integer> callback) {
        this.longPoll().registerChatCallback("OnChatLeaveCallback", callback);
    }

    public void onChatTitleChanged(CallbackFourth<String, String, Integer, Integer> callback) {
        this.longPoll().registerChatCallback("OnChatTitleChangedCallback", callback);
    }

    public void onChatPhotoChanged(CallbackTriple<JSONObject, Integer, Integer> callback) {
        this.longPoll().registerChatCallback("onChatPhotoChangedCallback", callback);
    }

    public void onChatPhotoRemoved(CallbackDouble<Integer, Integer> callback) {
        this.longPoll().registerChatCallback("onChatPhotoRemovedCallback", callback);
    }

    public void onChatCreated(CallbackTriple<String, Integer, Integer> callback) {
        this.longPoll().registerChatCallback("onChatCreatedCallback", callback);
    }

    /* Messages */
    public void onChatMessage(Callback<Message> callback) {
        this.longPoll().registerCallback("OnChatMessageCallback", callback);
    }

    public void onEveryMessage(Callback<Message> callback) {
        this.longPoll().registerCallback("OnEveryMessageCallback", callback);
    }

    public void onMessageWithFwds(Callback<Message> callback) {
        this.longPoll().registerCallback("OnMessageWithFwdsCallback", callback);
    }

    public void onAudioMessage(Callback<Message> callback) {
        this.longPoll().registerCallback("OnAudioMessageCallback", callback);
    }

    public void onDocMessage(Callback<Message> callback) {
        this.longPoll().registerCallback("OnDocMessageCallback", callback);
    }

    public void onGifMessage(Callback<Message> callback) {
        this.longPoll().registerCallback("OnGifMessageCallback", callback);
    }

    public void onLinkMessage(Callback<Message> callback) {
        this.longPoll().registerCallback("OnLinkMessageCallback", callback);
    }

    public void onMessage(Callback<Message> callback) {
        this.longPoll().registerCallback("OnMessageCallback", callback);
    }

    public void onPhotoMessage(Callback<Message> callback) {
        this.longPoll().registerCallback("OnPhotoMessageCallback", callback);
    }

    public void onSimpleTextMessage(Callback<Message> callback) {
        this.longPoll().registerCallback("OnSimpleTextMessageCallback", callback);
    }

    public void onStickerMessage(Callback<Message> callback) {
        this.longPoll().registerCallback("OnStickerMessageCallback", callback);
    }

    public void onTyping(Callback<Integer> callback) {
        this.longPoll().registerCallback("OnTypingCallback", callback);
    }

    public void onVideoMessage(Callback<Message> callback) {
        this.longPoll().registerCallback("OnVideoMessageCallback", callback);
    }

    public void onVoiceMessage(Callback<Message> callback) {
        this.longPoll().registerCallback("OnVoiceMessageCallback", callback);
    }

    public void onWallMessage(Callback<Message> callback) {
        this.longPoll().registerCallback("OnWallMessageCallback", callback);
    }

    /* Other */
    public void onFriendOnline(CallbackDouble<Integer, Integer> callback) {
        this.longPoll().registerAbstractCallback("OnFriendOnlineCallback", callback);
    }

    public void onFriendOffline(CallbackDouble<Integer, Integer> callback) {
        this.longPoll().registerAbstractCallback("OnFriendOfflineCallback", callback);
    }

    /* Commands */

    public void onCommand(Object command, Callback<Message> callback) {
        this.commands.add(new Command(command, callback));
    }

    public void onCommand(Callback<Message> callback, Object... commands) {
        this.commands.add(new Command(commands, callback));
    }

    public void onCommand(Object[] commands, Callback<Message> callback) {
        this.commands.add(new Command(commands, callback));
    }

    public void onCommand(List<?> list, Callback<Message> callback) {
        this.commands.add(new Command(list, callback));
    }


    /**
     * If true, all updates from longpoll server
     * will be logged to level 'INFO'
     * @param enable true or false
     */
    public void enableLoggingUpdates(boolean enable) {
        this.longPoll().enableLoggingUpdates(enable);
    }

    /**
     * Command object
     */
    public static class Command {
        private final Object[] commands;
        private final Callback<Message> callback;

        public Command(Object[] commands, Callback<Message> callback) {
            this.commands = commands;
            this.callback = callback;
        }

        public Command(Object command, Callback<Message> callback) {
            this.commands = new Object[]{command};
            this.callback = callback;
        }

        public Command(List<?> command, Callback<Message> callback) {
            this.commands = command.toArray();
            this.callback = callback;
        }

        public Object[] getCommands() {
            return commands;
        }

        public Callback<Message> getCallback() {
            return callback;
        }
    }

    // Getters and setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getToken() {
        return this.token;
    }

    @Override
    public String toString() {
        return String.format("{\"id\": %s}", id);
    }
}
