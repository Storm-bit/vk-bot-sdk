package com.github.stormbit.sdk.longpoll;

import com.github.stormbit.sdk.callbacks.*;
import com.github.stormbit.sdk.clients.Client;
import com.github.stormbit.sdk.objects.Chat;
import com.github.stormbit.sdk.objects.Message;
import org.json.JSONObject;
import static com.github.stormbit.sdk.clients.Client.service;

/**
 * Created by Storm-Bit on 28/09/2017 21:59
 *
 * Class for handling all updates in other thread
 */
public class UpdatesHandler2 extends UpdatesHandler {

    UpdatesHandler2(Client client) {
        super(client);
    }

    protected void handleCurrentUpdate() {

        JSONObject currentUpdate;

        if (this.queue.updates2.isEmpty()) {
            return;
        } else {
            currentUpdate = this.queue.shift2();
        }

        Events updateType = Events.get(currentUpdate.getString("type"));

        JSONObject object = currentUpdate.getJSONObject("object");

        if (callbacks.containsKey(updateType.getType())) {
            callbacks.get(updateType.getType()).onResult(object);
        }

        switch (updateType) {

            // Handling new message
            case MESSAGE_NEW: {

                // check if message is received
                service.submit(() -> handleMessageUpdate(object));

                // handle every
                handleEveryLongPollUpdate(object);
                break;
            }

            // Handling update (user started typing)
            case MESSAGE_TYPING_STATE: {
                handleTypingUpdate(object);

                // handle every
                handleEveryLongPollUpdate(currentUpdate);
                break;
            }

            // Handling other
            default: {
                handleEveryLongPollUpdate(currentUpdate);
            }
        }
    }

    /**
     * Handle chat events
     */
    @SuppressWarnings("unchecked")
    private void handleChatEvents(JSONObject updateObject) {

        Integer chatId = updateObject.getInt("peer_id");

        JSONObject attachments = updateObject.getJSONArray("attachments").length() > 0 ? updateObject.getJSONArray("attachments").getJSONObject(0) : null;

        // Return if no attachments
        // Because there no events,
        // and because simple chat messages will be handled
        if (attachments == null) return;

        if (attachments.has("source_act")) {
            String sourceAct = attachments.getString("source_act");

            Integer from = Integer.parseInt(attachments.getString("from"));

            switch (sourceAct) {
                case "chat_create": {

                    String title = attachments.getString("source_text");

                    if (chatCallbacks.containsKey("onChatCreatedCallback")) {
                        ((CallbackTriple<String, Integer, Integer>) chatCallbacks.get("onChatCreatedCallback")).onEvent(title, from, chatId);
                    }
                    break;
                }
                case "chat_title_update": {

                    String oldTitle = attachments.getString("source_old_text");
                    String newTitle = attachments.getString("source_text");

                    if (chatCallbacks.containsKey("OnChatTitleChangedCallback")) {
                        ((CallbackFourth<String, String, Integer, Integer>) chatCallbacks.get("OnChatTitleChangedCallback")).onEvent(oldTitle, newTitle, from, chatId);
                    }
                    break;
                }
                case "chat_photo_update": {

                    JSONObject photo = new JSONObject(client.api().callSync("messages.getById","message_ids", updateObject.getInt("conversation_message_id")).getJSONObject("response").getJSONArray("items").getJSONObject(0).getJSONArray("attachments").getJSONObject(0).getJSONObject("photo"));

                    if (chatCallbacks.containsKey("onChatPhotoChangedCallback")) {
                        ((CallbackTriple<JSONObject, Integer, Integer>) chatCallbacks.get("onChatPhotoChangedCallback")).onEvent(photo, from, chatId);
                    }

                    break;
                }
                case "chat_invite_user": {

                    Integer user = Integer.valueOf(attachments.getString("source_mid"));

                    if (chatCallbacks.containsKey("OnChatJoinCallback")) {
                        ((CallbackTriple<Integer, Integer, Integer>) chatCallbacks.get("OnChatJoinCallback")).onEvent(from, user, chatId);
                    }
                    break;
                }
                case "chat_kick_user": {

                    Integer user = Integer.valueOf(attachments.getString("source_mid"));

                    if (chatCallbacks.containsKey("OnChatLeaveCallback")) {
                        ((CallbackTriple<Integer, Integer, Integer>) chatCallbacks.get("OnChatLeaveCallback")).onEvent(from, user, chatId);
                    }
                    break;
                }
                case "chat_photo_remove": {

                    if (chatCallbacks.containsKey("onChatPhotoRemovedCallback")) {
                        ((CallbackDouble<Integer, Integer>) chatCallbacks.get("onChatPhotoRemovedCallback")).onEvent(from, chatId);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Handle every longpoll event
     */
    @SuppressWarnings("unchecked")
    private void handleEveryLongPollUpdate(JSONObject updateObject) {
        if (callbacks.containsKey("OnEveryLongPollEventCallback")) {
            callbacks.get("OnEveryLongPollEventCallback").onResult(updateObject);
        }
    }

    /**
     * Handle new message
     */
    @SuppressWarnings("unchecked")
    private void handleMessageUpdate(JSONObject updateObject) {

        // Flag
        boolean messageIsAlreadyHandled = false;

        // All necessary data
        int messageId = updateObject.getInt("id"),
                peerId = updateObject.getInt("peer_id"),
                chatId = 0,
                timestamp = updateObject.getInt("date");

        String messageText = updateObject.getString("text");

        JSONObject attachments = updateObject.getJSONArray("attachments").length() > 0 ? updateObject.getJSONArray("attachments").getJSONObject(0) : null;

        Integer randomId = updateObject.getInt("random_id");

        // Check for chat
        if (peerId > Chat.CHAT_PREFIX) {
            chatId = peerId - Chat.CHAT_PREFIX;
            if (attachments != null) {
                peerId = Integer.parseInt(attachments.getString("from"));
            }
            messageId = updateObject.getInt("conversation_message_id");
        }

        attachments = new JSONObject();

        Message message = new Message(
                this.client,
                messageId,
                peerId,
                timestamp,
                messageText,
                attachments,
                randomId
        );

        if (chatId > 0) {
            message.setChatId(chatId);
            message.setChatIdLong(Chat.CHAT_PREFIX + chatId);

            // chat events
            handleChatEvents(updateObject);
        }

        // check for commands
        if (this.client.commands.size() > 0) {
            messageIsAlreadyHandled = handleCommands(message);
        }

        if (message.hasFwds()) {
            if (callbacks.containsKey("OnMessageWithFwdsCallback")) {
                callbacks.get("OnMessageWithFwdsCallback").onResult(message);
                messageIsAlreadyHandled = true;

                handleSendTyping(message);
            }
        }

        if (!messageIsAlreadyHandled) {
            switch (message.messageType()) {

                case "voiceMessage": {
                    if (callbacks.containsKey("OnVoiceMessageCallback")) {
                        callbacks.get("OnVoiceMessageCallback").onResult(message);
                        messageIsAlreadyHandled = true;

                        handleSendTyping(message);
                    }
                    break;
                }

                case "stickerMessage": {
                    if (callbacks.containsKey("OnStickerMessageCallback")) {
                        callbacks.get("OnStickerMessageCallback").onResult(message);
                        messageIsAlreadyHandled = true;

                        handleSendTyping(message);
                    }
                    break;
                }

                case "gifMessage": {
                    if (callbacks.containsKey("OnGifMessageCallback")) {
                        callbacks.get("OnGifMessageCallback").onResult(message);
                        messageIsAlreadyHandled = true;

                        handleSendTyping(message);
                    }
                    break;
                }

                case "audioMessage": {
                    if (callbacks.containsKey("OnAudioMessageCallback")) {
                        callbacks.get("OnAudioMessageCallback").onResult(message);
                        messageIsAlreadyHandled = true;

                        handleSendTyping(message);
                    }
                    break;
                }

                case "videoMessage": {
                    if (callbacks.containsKey("OnVideoMessageCallback")) {
                        callbacks.get("OnVideoMessageCallback").onResult(message);
                        messageIsAlreadyHandled = true;

                        handleSendTyping(message);
                    }
                    break;
                }

                case "docMessage": {
                    if (callbacks.containsKey("OnDocMessageCallback")) {
                        callbacks.get("OnDocMessageCallback").onResult(message);
                        messageIsAlreadyHandled = true;

                        handleSendTyping(message);
                    }
                    break;
                }

                case "wallMessage": {
                    if (callbacks.containsKey("OnWallMessageCallback")) {
                        callbacks.get("OnWallMessageCallback").onResult(message);
                        messageIsAlreadyHandled = true;

                        handleSendTyping(message);
                    }
                    break;
                }

                case "photoMessage": {
                    if (callbacks.containsKey("OnPhotoMessageCallback")) {
                        callbacks.get("OnPhotoMessageCallback").onResult(message);
                        messageIsAlreadyHandled = true;

                        handleSendTyping(message);
                    }
                    break;
                }

                case "linkMessage": {
                    if (callbacks.containsKey("OnLinkMessageCallback")) {
                        callbacks.get("OnLinkMessageCallback").onResult(message);
                        messageIsAlreadyHandled = true;

                        handleSendTyping(message);
                    }
                    break;
                }

                case "simpleTextMessage": {
                    if (callbacks.containsKey("OnSimpleTextMessageCallback")) {
                        callbacks.get("OnSimpleTextMessageCallback").onResult(message);
                        messageIsAlreadyHandled = true;

                        handleSendTyping(message);
                    }
                    break;
                }
            }
        }

        if (callbacks.containsKey("OnMessageCallback") && !messageIsAlreadyHandled) {
            callbacks.get("OnMessageCallback").onResult(message);

            handleSendTyping(message);
        }

        if (callbacks.containsKey("OnChatMessageCallback") && !messageIsAlreadyHandled) {
            callbacks.get("OnChatMessageCallback").onResult(message);
        }

        if (callbacks.containsKey("OnEveryMessageCallback")) {
            callbacks.get("OnEveryMessageCallback").onResult(message);

            handleSendTyping(message);
        }
    }

    /**
     * Handle dialog with typing user
     */
    @SuppressWarnings("unchecked")
    private void handleTypingUpdate(JSONObject updateObject) {

        if (callbacks.containsKey("OnTypingCallback")) {
            callbacks.get("OnTypingCallback").onResult(updateObject.getString("from_id"));
        }
    }

    /**
     * Handle message and call back if it contains any command
     *
     * @param message received message
     */
    private boolean handleCommands(Message message) {

        boolean is = false;

        for (Client.Command command : this.client.commands) {
            for (int i = 0; i < command.getCommands().length; i++) {
                if (message.getText().toLowerCase().contains(command.getCommands()[i].toString().toLowerCase())) {
                    command.getCallback().onResult(message);
                    is = true;

                    handleSendTyping(message);
                }
            }
        }

        return is;
    }

    /**
     * Send typing
     */
    private void handleSendTyping(Message message) {

        // Send typing
        if (sendTyping) {
            if (!message.isMessageFromChat()) {
                this.client.api().call("messages.setActivity", "{type:'typing',peer_id:" + message.authorId() + "}", response -> {
                });
            } else {
                this.client.api().call("messages.setActivity", "{type:'typing',peer_id:" + message.getChatIdLong() + "}", response -> {
                });
            }
        }
    }
}
