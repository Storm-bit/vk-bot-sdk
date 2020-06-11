package com.github.stormbit.sdk.longpoll;

import com.github.stormbit.sdk.callbacks.*;
import com.github.stormbit.sdk.clients.Client;
import com.github.stormbit.sdk.objects.Chat;
import com.github.stormbit.sdk.objects.Message;
import org.json.JSONArray;
import org.json.JSONObject;

import static com.github.stormbit.sdk.clients.Client.service;

/**
 * Created by Storm-bit on 03/04/2020 19:40
 *
 * Class for handling all updates in other thread
 */
public class UpdatesHandler1 extends UpdatesHandler {

    /**
     * Client with access_token
     */
    UpdatesHandler1(Client client) {
        super(client);
    }

    @Override
    protected void handleCurrentUpdate() {

        JSONArray currentUpdate;

        if (this.queue.updates.isEmpty()) {
            return;
        } else {
            currentUpdate = this.queue.shift();
        }

        int updateType = currentUpdate.getInt(0);

        switch (updateType) {

            // Handling new message
            case 4: {

                int messageFlags = currentUpdate.getInt(2);

                // check if message is received
                if ((messageFlags & 2) == 0) {
                    service.submit(() -> handleMessageUpdate(currentUpdate));
                }

                // handle every
                handleEveryLongPollUpdate(currentUpdate);
                break;
            }

            // Handling update (user started typing)
            case 62: {
                handleTypingUpdate(currentUpdate);

                // handle every
                handleEveryLongPollUpdate(currentUpdate);
                break;
            }

            // Handling friend online
            case 8: {
                handleOnline(currentUpdate);

                //handle every
                handleEveryLongPollUpdate(currentUpdate);
                break;
            }

            // Handling friend offline
            case 9: {
                handleOffline(currentUpdate);

                //handle every
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
    private void handleChatEvents(JSONArray updateObject) {

        Integer chatId = updateObject.getInt(3);

        JSONObject attachments = (updateObject.length() > 6 ? (updateObject.get(6).toString().startsWith("{") ? new JSONObject(updateObject.get(6).toString()) : null) : null);

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

                    JSONObject photo = new JSONObject(client.api().callSync("messages.getById", client, "message_ids", updateObject.getInt(1))).getJSONObject("response").getJSONArray("items").getJSONObject(0).getJSONArray("attachments").getJSONObject(0).getJSONObject("photo");

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
    private void handleEveryLongPollUpdate(JSONArray updateObject) {
        if (callbacks.containsKey("OnEveryLongPollEventCallback")) {
            callbacks.get("OnEveryLongPollEventCallback").onResult(updateObject);
        }
    }

    /**
     * Handle new message
     */
    @SuppressWarnings("unchecked")
    private void handleMessageUpdate(JSONArray updateObject) {

        // Flag
        boolean messageIsAlreadyHandled = false;

        // All necessary data
        int messageId = updateObject.getInt(1),
                peerId = updateObject.getInt(3),
                chatId = 0,
                timestamp = updateObject.getInt(4);

        String messageText = updateObject.getString(5);

        JSONObject attachments = (updateObject.length() > 6 ? (updateObject.get(6).toString().startsWith("{") ? new JSONObject(updateObject.get(6).toString()) : null) : null);

        Integer randomId = updateObject.length() > 7 ? updateObject.getInt(8) : null;

        // Check for chat
        if (peerId > Chat.CHAT_PREFIX) {
            chatId = peerId - Chat.CHAT_PREFIX;
            if (attachments != null) {
                peerId = Integer.parseInt(attachments.getString("from"));
            }
        }

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

    private void handleOnline(JSONArray updateObject) {
        Integer targetId = updateObject.getInt(1),
                timestamp = updateObject.getInt(3);


        if (callbacks.containsKey("OnFriendOnlineCallback")) {
            ((CallbackDouble<Integer, Integer>) abstractCallbacks.get("OnFriendOnlineCallback")).onEvent(targetId, timestamp);
        }
    }

    private void handleOffline(JSONArray updateObject) {
        Integer targetId = updateObject.getInt(1),
                timestamp = updateObject.getInt(3);

        if (callbacks.containsKey("OnFriendOfflineCallback")) {
            ((CallbackDouble<Integer, Integer>) abstractCallbacks.get("OnFriendOfflineCallback")).onEvent(targetId, timestamp);
        }
    }

    /**
     * Handle dialog with typing user
     */
    @SuppressWarnings("unchecked")
    private void handleTypingUpdate(JSONArray updateObject) {

        if (callbacks.containsKey("OnTypingCallback")) {
            callbacks.get("OnTypingCallback").onResult(updateObject.getInt(1));
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
