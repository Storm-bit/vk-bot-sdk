package com.github.stormbit.sdk.clients;

import com.github.stormbit.sdk.callbacks.Callback;
import com.github.stormbit.sdk.callbacks.CallbackDouble;
import com.github.stormbit.sdk.callbacks.CallbackFourth;
import com.github.stormbit.sdk.callbacks.CallbackTriple;
import com.github.stormbit.sdk.longpoll.LongPoll;
import com.github.stormbit.sdk.objects.Chat;
import com.github.stormbit.sdk.objects.Message;
import com.github.stormbit.sdk.utils.vkapi.API;
import com.github.stormbit.sdk.utils.vkapi.Auth;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.*;

/**
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
    private Auth _auth;

    public CopyOnWriteArrayList<Command> commands = new CopyOnWriteArrayList<>();
    private ConcurrentHashMap<Integer, Chat> chats = new ConcurrentHashMap<>();


    Client(String login, String password, int id) {
        _auth = new Auth(login, password);
        _auth.auth();
        this.id = id;
        api = new API(this);

        this.longPoll = new LongPoll(this);
    }

    /**
     * Default constructor
     *
     * @param id           User or group id
     * @param access_token Access token key
     */
    Client(Integer id, String access_token, String login, String password) {
        _auth = new Auth(login, password);
        _auth.auth();

        this.id = id;
        this.longPoll = new LongPoll(this);

        api = new API(this);
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
     */
    public LongPoll longPoll() {
        return longPoll;
    }

    public Auth get_auth() {
        return _auth;
    }

    /**
     * Get API for making requests
     */
    public API api() {
        return api;
    }

    /**
     * If the client need to start typing
     * after receiving message
     * and until client's message is sent
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
     */
    public void enableLoggingUpdates(boolean enable) {
        this.longPoll().enableLoggingUpdates(enable);
    }

    /**
     * Command object
     */
    public class Command {
        private Object[] commands;
        private Callback<Message> callback;

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

    @Override
    public String toString() {
        return "";
    }
}
