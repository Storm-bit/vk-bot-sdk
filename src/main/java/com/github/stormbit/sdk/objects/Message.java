package com.github.stormbit.sdk.objects;

import com.github.stormbit.sdk.callbacks.Callback;
import com.github.stormbit.sdk.clients.Client;
import com.github.stormbit.sdk.utils.vkapi.API;
import com.github.stormbit.sdk.utils.vkapi.Upload;
import com.github.stormbit.sdk.utils.vkapi.docs.DocTypes;
import com.github.stormbit.sdk.utils.vkapi.keyboard.Keyboard;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Message object for both (received and sent) messages
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Message {

    private static final Logger LOG = LoggerFactory.getLogger(Message.class);

    private Integer messageId, peerId, timestamp, randomId, stickerId, chatId, chatIdLong;
    private Keyboard keyboard;
    private JSONObject payload = new JSONObject();
    private String text, title;
    private API api;
    private Upload upload;
    private Client client;

    /**
     * Attachments in format of received event from longpoll server
     * More: <a href="https://vk.com/dev/using_longpoll_2">link</a>
     */
    private JSONObject attachmentsOfReceivedMessage = new JSONObject();

    /**
     * Attachments in format [photo62802565_456241137, photo111_111, doc100_500]
     */
    private CopyOnWriteArrayList<String> attachments = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<String> forwardedMessages = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<String> photosToUpload = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<JSONObject> docsToUpload = new CopyOnWriteArrayList<>();

    /**
     * Constructor for sent message
     */
    public Message() {

    }

    /**
     * Constructor for received message
     * @param client client
     * @param messageId message id
     * @param peerId peer id
     * @param timestamp timestamp
     * @param text message text
     * @param attachments message attachments
     * @param randomId random id
     */
    public Message(Client client, Integer messageId, Integer peerId, Integer timestamp, String text, JSONObject attachments, Integer randomId, JSONObject payload) {

        setMessageId(messageId);
        setPeerId(peerId);
        setTimestamp(timestamp);
        setText(text);
        setAttachments(attachments);
        setRandomId(randomId);
        setPayload(payload);
        setTitle(attachments != null && attachments.has("title") ? attachments.getString("title") : " ... ");
        this.client = client;
        api = client.api();
        upload = new Upload(client);
    }

    /**
     * Your client with id
     * @param client client
     * @return this
     */
    public Message from(Client client) {
        api = client.api();
        upload = new Upload(client);
        this.client = client;

        return this;
    }

    /**
     * ID of target dialog
     * @param peerId target
     * @return this
     */
    public Message to(Integer peerId) {
        this.peerId = peerId;
        return this;
    }

    /**
     * ID of sticker
     * @param id sticker id
     * @return this
     */
    public Message sticker(Integer id) {
        this.stickerId = id;
        return this;
    }

    /**
     * IDs of forwarded messages
     * @param ids message ids
     * @return this
     */
    public Message forwardedMessages(Object... ids) {

        for (Object id : ids) {
            this.forwardedMessages.add(String.valueOf(id));
        }
        return this;
    }

    /**
     * Message text
     * @param text message content
     * @return this
     */
    public Message text(Object text) {
        this.text = String.valueOf(text);
        return this;
    }

    /**
     * Message title (bold text)
     * @param title message title
     * @return this
     */
    public Message title(Object title) {
        this.title = String.valueOf(title);
        return this;
    }

    public Message keyboard(Keyboard button) {
        this.keyboard = button;
        return this;
    }

    /**
     * Message attachments
     * @param attachments attachments
     * @return this
     */
    public Message attachments(String... attachments) {

        if (attachments.length > 10)
            LOG.error("Trying to send message with illegal count of attachments: {} (> 10)", attachments.length);
        else if (attachments.length == 1 && attachments[0].contains(",")) {
            this.attachments.addAllAbsent(Arrays.asList(attachments[0].split(",")));
        } else {
            this.attachments.addAllAbsent(Arrays.asList(attachments));
        }
        return this;
    }

    /**
     * Message random_id
     * @param randomId random
     * @return this
     */
    public Message randomId(Integer randomId) {
        this.randomId = randomId;
        return this;
    }

    /**
     * @param photo String URL, link to vk doc or path to file
     * @return this
     */
    public Message photo(String photo) {
        if (Pattern.matches("[htps:/vk.com]?photo-?\\d+_\\d+", photo)) {
            attachments.add(photo.substring(photo.lastIndexOf("photo")));
            return this;
        }

        this.attachments.add(upload.uploadPhoto(photo, peerId));

        return this;
    }

    /**
     * Synchronous adding doc to the message
     *
     * @param doc String URL, link to vk doc or path to file
     * @return this
     */
    public Message doc(String doc) {
        String docAsAttach = upload.uploadDoc(doc, peerId, DocTypes.DOC);

        if (docAsAttach != null) this.attachments.add(docAsAttach);

        return this;
    }

    /**
     * Attach photo to message
     * <p>
     * Works slower that sync photo adding, but will be called from execute
     *
     * @param photo Photo link: url, from disk or already uploaded to VK as photo{owner_id}_{id}
     * @return this
     */
    public Message photoAsync(String photo) {

        // Use already loaded photo
        if (Pattern.matches("[htps:/vk.com]?photo-?\\d+_\\d+", photo)) {
            this.attachments.add(photo.substring(photo.lastIndexOf("photo")));
            return this;
        }

        // Use photo from url of disc
        this.photosToUpload.add(photo);

        return this;
    }

    /**
     * Attach doc to message
     *
     * @param doc Doc link: url, from disk or already uploaded to VK as doc{owner_id}_{id}
     * @param type type
     * @return this
     */
    public Message docAsync(String doc, DocTypes type) {

        // Use already loaded photo
        if (Pattern.matches("[htps:/vk.com]?doc-?\\d+_\\d+", doc)) {
            this.attachments.add(doc);
            return this;
        }

        this.docsToUpload.add(new JSONObject().put("doc", doc).put("type", type.getType()));

        return this;
    }

    /**
     * Attach doc to message
     *
     * @param doc Doc link: url, from disk or already uploaded to VK as doc{owner_id}_{id}
     * @return this
     */
    public Message docAsync(String doc) {
        this.docAsync(doc, DocTypes.DOC);

        return this;
    }

    /**
     * Send voice message
     *
     * @param doc      URL or path to file
     * @param callback response will returns to callback
     */
    public void sendVoiceMessage(String doc, Callback<Object>... callback) {
        String docAsAttach = upload.uploadDoc(doc, peerId, DocTypes.AUDIO_MESSAGE);

        if (docAsAttach != null) this.attachments.add(docAsAttach);

        send(callback);
    }

    /**
     * Send the message
     *
     * @param callback will be called with response object
     */
    public void send(Callback<Object>... callback) {

        if (photosToUpload.size() > 0) {
            String photo = photosToUpload.get(0);
            photosToUpload.remove(0);
            upload.uploadPhotoAsync(photo, peerId, response -> {
                if (!response.toString().equalsIgnoreCase("false")) {
                    this.attachments.addIfAbsent(response.toString());
                    send(callback);
                } else {
                    LOG.error("Some error occurred when uploading photo.");
                }
            });
            return;
        }

        if (docsToUpload.size() > 0) {
            JSONObject doc = docsToUpload.get(0);
            docsToUpload.remove(0);
            upload.uploadDocAsync(doc, peerId, response -> {
                if (!response.toString().equalsIgnoreCase("false")) {
                    this.attachments.addIfAbsent(response.toString());
                    send(callback);
                } else {
                    LOG.error("Some error occurred when uploading doc.");
                }
            });
            return;
        }

        text = (text != null && text.length() > 0) ? text : "";
        title = (title != null && title.length() > 0) ? title : "";

        randomId = randomId != null && randomId > 0 ? randomId : 0;
        attachments = attachments != null && attachments.size() > 0 ? attachments : new CopyOnWriteArrayList<>();
        forwardedMessages = forwardedMessages != null && forwardedMessages.size() > 0 ? forwardedMessages : new CopyOnWriteArrayList<>();
        stickerId = stickerId != null && stickerId > 0 ? stickerId : 0;

        JSONObject params = new JSONObject();

        if (!text.isEmpty()) params.put("message", text);
        if (title != null && title.length() > 0) params.put("title", title);
        if (randomId != null) params.put("random_id", randomId);
        params.put("peer_id", peerId);
        if (attachments.size() > 0) params.put("attachment", String.join(",", attachments));
        if (forwardedMessages.size() > 0) params.put("forward_messages", String.join(",", forwardedMessages));
        if (stickerId != null && stickerId > 0) params.put("sticker_id", stickerId);
        if (keyboard != null) params.put("keyboard", new JSONObject(keyboard));

        api.call("messages.send", params, response -> {
            if (callback.length > 0) {
                callback[0].onResult(response);
            }
            if (!(response instanceof Integer)) {
                LOG.error("Message not sent: {}", response);
            }
        });
    }

    /**
     * Get the type of message
     * @return type of message
     */
    public String messageType() {

        if (isVoiceMessage()) {
            return "voiceMessage";
        } else if (isStickerMessage()) {
            return "stickerMessage";
        } else if (isGifMessage()) {
            return "gifMessage";
        } else if (isAudioMessage()) {
            return "audioMessage";
        } else if (isVideoMessage()) {
            return "videoMessage";
        } else if (isDocMessage()) {
            return "docMessage";
        } else if (isWallMessage()) {
            return "wallMessage";
        } else if (isPhotoMessage()) {
            return "photoMessage";
        } else if (isLinkMessage()) {
            return "linkMessage";
        } else if (isSimpleTextMessage()) {
            return "simpleTextMessage";
        } else return "error";
    }

    /**
     * @return true if message has forwarded messages
     */
    public boolean hasFwds() {
        boolean answer = false;

        if (attachmentsOfReceivedMessage.has("fwd"))
            answer = true;

        return answer;
    }

    /**
     * @return array of forwarded messages or []
     */
    public JSONArray getForwardedMessages() {
        if (hasFwds()) {
            JSONObject response = api.callSync("messages.getById", "message_ids", getMessageId());

            if (response.has("response") && response.getJSONObject("response").getJSONArray("items").getJSONObject(0).has("fwd_messages")) {
                return response.getJSONObject("response").getJSONArray("items").getJSONObject(0).getJSONArray("fwd_messages");
            }
        }

        return new JSONArray();
    }

    /**
     * @return JSONObject with reply message or {}
     */
    public JSONObject getReplyMessage() {
        if (hasFwds()) {
            JSONObject response = api.callSync("messages.getById", "message_ids", getMessageId());

            if (response.has("response") && response.getJSONObject("response").getJSONArray("items").getJSONObject(0).has("reply_message")) {
                return response.getJSONObject("response").getJSONArray("items").getJSONObject(0).getJSONObject("reply_message");
            }
        }

        return new JSONObject();
    }

    /**
     * Get attachments from message
     * @return JSONArray attachments
     */
    public JSONArray getAttachments() {

        JSONObject response;
        if (isMessageFromChat()) {
            response = api.callSync("messages.getByConversationMessageId", "peer_id", chatIdLong, "conversation_message_ids", messageId, "group_id", client.getId());
        } else {
            response = api.callSync("messages.getById", "message_ids", getMessageId());
        }

        if (response.has("response") && response.getJSONObject("response").getJSONArray("items").length() > 0) {
            if (response.getJSONObject("response").getJSONArray("items").getJSONObject(0).has("attachments")) {
                return response.getJSONObject("response").getJSONArray("items").getJSONObject(0).getJSONArray("attachments");
            }
        }

        return new JSONArray();
    }

    /*
     * Priority: voice, sticker, gif, ... , simple text
     */
    public boolean isPhotoMessage() {
        return getCountOfAttachmentsByType().get("photo") > 0;
    }

    public boolean isSimpleTextMessage() {
        return getCountOfAttachmentsByType().get("summary") == 0;
    }

    public boolean isVoiceMessage() {
        return getCountOfAttachmentsByType().get("voice") > 0;
    }

    public boolean isAudioMessage() {
        return getCountOfAttachmentsByType().get("audio") > 0;
    }

    public boolean isVideoMessage() {
        return getCountOfAttachmentsByType().get("video") > 0;
    }

    public boolean isDocMessage() {
        return getCountOfAttachmentsByType().get("doc") > 0;
    }

    public boolean isWallMessage() {
        return getCountOfAttachmentsByType().get("wall") > 0;
    }

    public boolean isStickerMessage() {
        return getCountOfAttachmentsByType().get("sticker") > 0;
    }

    public boolean isLinkMessage() {
        return getCountOfAttachmentsByType().get("link") > 0;
    }

    public boolean isGifMessage() {
        JSONArray attachments = getAttachments();

        for (Object attachment : attachments) {
            if (attachment instanceof JSONObject) {
                var attachmentAsJson = (JSONObject) attachment;

                if (attachmentAsJson.has("type") && attachmentAsJson.getJSONObject(attachmentAsJson.getString("type")).has("type") && attachmentAsJson.getJSONObject(attachmentAsJson.getString("type")).getInt("type") == 3)
                    return true;
            }
        }

        return false;
    }

    // Getters and setters for handling new message

    /**
     * Method helps to identify kind of message
     *
     * @return Map: key=type of attachment, value=count of attachments, key=summary - value=count of all attachments.
     */
    public Map<String, Integer> getCountOfAttachmentsByType() {

        int photo = 0, video = 0, audio = 0, doc = 0, wall = 0, link = 0;

        Map<String, Integer> answer = new HashMap<>() {{
            put("photo", 0);
            put("video", 0);
            put("audio", 0);
            put("doc", 0);
            put("wall", 0);
            put("sticker", 0);
            put("link", 0);
            put("voice", 0);
            put("summary", 0);
        }};

        if (attachmentsOfReceivedMessage.toString().contains("sticker")) {
            answer.put("sticker", 1);
            answer.put("summary", 1);
            return answer;
        } else {
            if (attachmentsOfReceivedMessage.toString().contains("audiomsg")) {
                answer.put("voice", 1);
                answer.put("summary", 1);
                return answer;
            } else {
                for (String key : attachmentsOfReceivedMessage.keySet()) {
                    if (key.equals("type")) {

                        String value = attachmentsOfReceivedMessage.getString(key);
                        switch (value) {

                            case "photo": {
                                answer.put(value, ++photo);
                                break;
                            }
                            case "video": {
                                answer.put(value, ++video);
                                break;
                            }
                            case "audio": {
                                answer.put(value, ++audio);
                                break;
                            }
                            case "doc": {
                                answer.put(value, ++doc);
                                break;
                            }
                            case "wall": {
                                answer.put(value, ++wall);
                                break;
                            }
                            case "link": {
                                answer.put(value, ++link);
                                break;
                            }
                        }
                    }

                }
            }
        }

        int summary = 0;
        for (String key : answer.keySet()) {
            if (answer.get(key) > 0)
                summary++;
        }
        answer.put("summary", summary);

        return answer;
    }

    /* Public getters */

    public Integer getMessageId() {
        return messageId;
    }

    public Integer authorId() {
        return peerId;
    }

    public Integer getTimestamp() {
        return timestamp;
    }

    public String getText() {
        return text;
    }

    public JSONArray getPhotos() {
        JSONArray attachments = getAttachments();
        JSONArray answer = new JSONArray();

        for (int i = 0; i < attachments.length(); i++) {
            if (attachments.getJSONObject(i).getString("type").contains("photo"))
                answer.put(attachments.getJSONObject(i).getJSONObject("photo"));
        }

        return answer;
    }

    public Integer getChatIdLong() {
        return chatIdLong;
    }

    /* Private setters */

    private void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }

    private void setPeerId(Integer peerId) {
        this.peerId = peerId;
    }

    private void setTimestamp(Integer timestamp) {
        this.timestamp = timestamp;
    }

    private void setText(String text) {
        this.text = text;
    }

    public void setChatIdLong(Integer chatIdLong) {
        this.chatIdLong = chatIdLong;
    }

    /**
     * @param photos JSONArray with photo objects
     * @return URL of biggest image file
     */
    public String getBiggestPhotoUrl(JSONArray photos) {

        String currentBiggestPhoto;

        Map<Integer, String> sizes = new HashMap<>();

        for (Object object : photos) {
            if (object instanceof JSONObject) {
                int width = ((JSONObject) object).getInt("width");
                String url = ((JSONObject) object).getString("url");
                sizes.put(width, url);
            }
        }

        currentBiggestPhoto = sizes.get(Collections.max(sizes.keySet()));

        return currentBiggestPhoto;
    }

    public JSONObject getVoiceMessage() {

        JSONArray attachments = getAttachments();
        JSONObject answer = new JSONObject();

        for (int i = 0; i < attachments.length(); i++) {
            if (attachments.getJSONObject(i).getString("type").contains("doc") && attachments.getJSONObject(i).getJSONObject("doc").toString().contains("waveform"))
                answer = attachments.getJSONObject(i).getJSONObject("doc");
        }

        return answer;
    }

    public boolean isMessageFromChat() {

        return (chatId != null && chatId > 0) || (chatIdLong != null && chatIdLong > 0);
    }

    public Integer chatId() {
        return chatId;
    }

    public void setChatId(Integer chatId) {
        this.chatId = chatId;
    }

    private void setAttachments(JSONObject attachments) {
        if (attachments == null) return;
        this.attachmentsOfReceivedMessage = attachments;
    }

    public Integer getRandomId() {
        return randomId;
    }

    private void setRandomId(Integer randomId) {
        this.randomId = randomId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Keyboard getKeyboard() {
        return keyboard;
    }

    public void setKeyboard(Keyboard keyboard) {
        this.keyboard = keyboard;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public void setPayload(JSONObject payload) {
        if (payload == null) return;

        this.payload = payload;
    }

    private String[] getForwardedMessagesIds() {

        if (attachmentsOfReceivedMessage.has("fwd")) {
            return attachmentsOfReceivedMessage.getString("fwd").split(",");
        }

        return new String[]{};
    }

    @Override
    public String toString() {
        return '{' +
                "\"message_id\":" + messageId +
                ",\"peer_id\":" + peerId +
                ",\"timestamp\":" + timestamp +
                ",\"random_id\":" + randomId +
                ",\"text\":\"" + text + '\"' +
                ",\"attachments\":" + attachmentsOfReceivedMessage.toString() +
                ",\"payload\":" + payload.toString() +
                '}';
    }
}
