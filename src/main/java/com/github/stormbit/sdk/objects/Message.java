package com.github.stormbit.sdk.objects;

import com.github.stormbit.sdk.callbacks.Callback;
import com.github.stormbit.sdk.clients.Client;
import com.github.stormbit.sdk.utils.Utils;
import com.github.stormbit.sdk.utils.vkapi.API;
import com.github.stormbit.sdk.utils.vkapi.docs.DocTypes;
import com.github.stormbit.sdk.utils.web.MultipartUtility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Message object for both (received and sent) messages
 */
public class Message {

    private static final Logger LOG = LoggerFactory.getLogger(Message.class);

    private Integer messageId, peerId, timestamp, randomId, stickerId, chatId, chatIdLong;
    private String text, title;
    private API api;
    private Client _client;

    /**
     * Attachments in format of received event from longpoll server
     * More: <a href="https://vk.com/dev/using_longpoll_2">link</a>
     */
    private JSONObject attachmentsOfReceivedMessage;

    /**
     * Attachments in format [photo62802565_456241137, photo111_111, doc100_500]
     */
    private CopyOnWriteArrayList<String> attachments = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<String> forwardedMessages = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<String> photosToUpload = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<JSONObject> docsToUpload = new CopyOnWriteArrayList<>();

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
    public Message(Client client, Integer messageId, Integer peerId, Integer timestamp, String text, JSONObject attachments, Integer randomId) {

        setMessageId(messageId);
        setPeerId(peerId);
        setTimestamp(timestamp);
        setText(text);
        setAttachments(attachments);
        setRandomId(randomId);
        setTitle(attachments.has("title") ? attachments.getString("title") : " ... ");
        _client = client;
        api = client.api();
    }

    /**
     * Your client with id
     * @param client client
     * @return this
     */
    public Message from(Client client) {
        api = client.api();
        _client = client;
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
     * Synchronous adding photo to the message
     *
     * @param photo String URL, link to vk doc or path to file
     * @return this
     */
    public Message photo(String photo) {

        // Use already loaded photo
        if (Pattern.matches("[htps:/vk.com]?photo-?\\d+_\\d+", photo)) {
            this.attachments.add(photo.substring(photo.lastIndexOf("photo")));
            return this;
        }

        String type = null;
        File photoFile = new File(photo);
        if (photoFile.exists()) {
            type = "fromFile";
        }

        URL photoUrl = null;
        if (type == null) {
            try {
                photoUrl = new URL(photo);
                type = "fromUrl";
            } catch (MalformedURLException ignored) {
                LOG.error("Error when trying add photo to message: file not found, or url is bad. Your param: {}", photo);
                return this;
            }
        }

        byte[] photoBytes;

        switch (type) {

            case "fromFile": {
                try {
                    photoBytes = Files.readAllBytes(Paths.get(photoFile.toURI()));
                } catch (IOException ignored) {
                    LOG.error("Error when reading file {}", photoFile.getAbsolutePath());
                    return this;
                }
                break;
            }

            case "fromUrl": {
                try {
                    photoBytes = Utils.toByteArray(photoUrl);
                } catch (IOException e) {
                    LOG.error("Error {} occured when reading URL {}", e.toString(), photo);
                    return this;
                }
                break;
            }

            default: {
                LOG.error("Bad 'photo' string: path to file, URL or already uploaded 'photo()_()' was expected.");
                return this;
            }
        }

        if (photoBytes != null) {

            // Getting of server for uploading the photo
            JSONObject getUploadServerResponse = api.callSync("photos.getMessagesUploadServer", _client, "peer_id", this.peerId);
            String uploadUrl = getUploadServerResponse.has("response") ? getUploadServerResponse.getJSONObject("response").has("upload_url") ? getUploadServerResponse.getJSONObject("response").getString("upload_url") : null : null;

            // Some error
            if (uploadUrl == null) {
                LOG.error("No upload url in response: {}", getUploadServerResponse);
                return this;
            }

            // if album_id == 3 make it negative
            uploadUrl = uploadUrl.replaceAll("aid=3", String.format("aid=%s", -getUploadServerResponse.getJSONObject("response").getInt("album_id")));

            String mimeType = "png";

            try {
                InputStream is = new BufferedInputStream(new ByteArrayInputStream(photoBytes));
                mimeType = URLConnection.guessContentTypeFromStream(is);
                mimeType = mimeType.substring(mimeType.lastIndexOf('/')+1).replace("jpeg", "jpg");
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }

            // Uploading the photo
            MultipartUtility multipartUtility = new MultipartUtility(uploadUrl);
            multipartUtility.addBytesPart("photo", "photo."+mimeType, photoBytes);
            String uploadingOfPhotoResponseString = multipartUtility.finish();

            JSONObject uploadingOfPhotoResponse;

            try {
                uploadingOfPhotoResponse = new JSONObject(uploadingOfPhotoResponseString);
            } catch (JSONException e) {
                LOG.error("Bad response of uploading photo: {}, error: {}", uploadingOfPhotoResponseString, e.toString());
                return this;
            }

            // Getting necessary params
            String server, photo_param, hash;
            if (uploadingOfPhotoResponse.has("server") && uploadingOfPhotoResponse.has("photo") && uploadingOfPhotoResponse.has("hash")) {
                server = "" + uploadingOfPhotoResponse.getInt("server");
                photo_param = uploadingOfPhotoResponse.get("photo").toString();
                hash = uploadingOfPhotoResponse.getString("hash");
            } else {
                LOG.error("No 'photo', 'server' or 'hash' param in response {}", uploadingOfPhotoResponseString);
                return this;
            }

            // Saving the photo
            JSONObject saveMessagesPhotoResponse = api.callSync("photos.saveMessagesPhoto", _client, "server", server, "photo", photo_param, "hash", hash);

            String photoAsAttach = saveMessagesPhotoResponse.has("response") ? "photo" + saveMessagesPhotoResponse.getJSONArray("response").getJSONObject(0).getInt("owner_id") + "_" + saveMessagesPhotoResponse.getJSONArray("response").getJSONObject(0).getInt("id") : "";

            this.attachments.add(photoAsAttach);
        }
        return this;
    }

    /**
     * Synchronous adding doc to the message
     * @param doc       String URL, link to vk doc or path to file
     * @param typeOfDoc Type of doc, 'audio_message' or 'graffiti' ('doc' as default)
     * @return this
     */
    public Message doc(String doc, DocTypes typeOfDoc) {

        // Use already loaded photo
        if (Pattern.matches("[htps:/vk.com]?doc-?\\d+_\\d+", doc)) {
            this.attachments.add(doc.substring(doc.lastIndexOf("doc")));
            return this;
        }

        String type = null;
        File docFile = new File(doc);
        if (docFile.exists()) {
            type = "fromFile";
        }

        URL docUrl = null;
        if (type == null) {
            try {
                docUrl = new URL(doc);
                type = "fromUrl";
            } catch (MalformedURLException ignored) {
                LOG.error("Error when trying add doc to message: file not found, or url is bad. Your param: {}", doc);
                return this;
            }
        }

        byte[] docBytes;
        String fileNameField;

        switch (type) {

            case "fromFile": {
                try {
                    docBytes = Files.readAllBytes(Paths.get(docFile.toURI()));
                    fileNameField = docFile.getName();
                } catch (IOException e) {
                    LOG.error("Error when reading file {}", docFile.getAbsolutePath());
                    return this;
                }
                break;
            }

            case "fromUrl": {
                try {
                    URLConnection conn = docUrl.openConnection();

                    try {
                        docBytes = Utils.toByteArray(conn);
                        fileNameField = Utils.guessFileNameByContentType(conn.getContentType());
                    } finally {
                        Utils.close(conn);
                    }
                } catch (IOException e) {
                    LOG.error("Error {} occured when reading URL {}", e.toString(), doc);
                    return this;
                }
                break;
            }

            default: {
                LOG.error("Bad 'doc' string: path to file, URL or already uploaded 'doc()_()' was expected, but got this: {}", doc);
                return this;
            }
        }

        docFromBytes(docBytes, typeOfDoc, fileNameField);

        return this;
    }

    public Message docFromBytes(byte[] docBytes, DocTypes typeOfDoc, String fileNameField) {

        if (docBytes != null) {

            // Getting of server for uploading the photo
            JSONObject getUploadServerResponse = api.callSync("docs.getMessagesUploadServer", _client, "peer_id", this.peerId, "type", typeOfDoc.getType());

            String uploadUrl = getUploadServerResponse.has("response") ? getUploadServerResponse.getJSONObject("response").has("upload_url") ? getUploadServerResponse.getJSONObject("response").getString("upload_url") : null : null;

            // Some error
            if (uploadUrl == null) {
                LOG.error("No upload url in response: {}", getUploadServerResponse);
                return this;
            }

            // Uploading the photo
            String uploadingOfDocResponseString;

            MultipartUtility multipartUtility = new MultipartUtility(uploadUrl);
            multipartUtility.addBytesPart("file", fileNameField, docBytes);
            uploadingOfDocResponseString = multipartUtility.finish();

            JSONObject uploadingOfDocResponse;

            try {
                uploadingOfDocResponse = new JSONObject(uploadingOfDocResponseString);
            } catch (JSONException e) {
                LOG.error("Bad response of uploading doc: {}, error: {}", uploadingOfDocResponseString, e.toString());
                return this;
            }

            // Getting necessary params
            String file;
            if (uploadingOfDocResponse.has("file")) {
                file = uploadingOfDocResponse.getString("file");
            } else {
                LOG.error("No 'file' param in response {}", uploadingOfDocResponseString);
                return this;
            }

            // Saving the photo
            JSONObject saveMessagesDocResponse = api.callSync("docs.save", _client, "file", file);
            String docAsAttach = saveMessagesDocResponse.has("response") ? "doc" + saveMessagesDocResponse.getJSONArray("response").getJSONObject(0).getInt("owner_id") + "_" + saveMessagesDocResponse.getJSONArray("response").getJSONObject(0).getInt("id") : "";

            this.attachments.add(docAsAttach);
        } else {
            LOG.error("Got file or url of doc to be uploaded, but some error occured and readed 0 bytes.");
        }

        return this;
    }

    /**
     * Synchronous adding doc to the message
     *
     * @param doc String URL, link to vk doc or path to file
     * @return this
     */
    public Message doc(String doc) {
        this.doc(doc, DocTypes.DOC);
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
     * Async uploading photos
     * @param photo Photo link: url, from disk or already uploaded to VK as photo{owner_id}_{id}
     * @param callback callback
     */
    public void uploadPhoto(String photo, Callback<Object> callback) {

        String type = null;
        File photoFile = new File(photo);
        if (photoFile.exists()) {
            type = "fromFile";
        }

        URL photoUrl = null;
        if (type == null) {
            try {
                photoUrl = new URL(photo);
                type = "fromUrl";
            } catch (MalformedURLException ignored) {
                LOG.error("Error when trying add photo to message: file not found, or url is bad. Your param: {}", photo);
                callback.onResult("false");
                return;
            }
        }

        byte[] photoBytes;
        switch (type) {

            case "fromFile": {
                try {
                    photoBytes = Files.readAllBytes(Paths.get(photoFile.toURI()));
                } catch (IOException ignored) {
                    LOG.error("Error when reading file {}", photoFile.getAbsolutePath());
                    callback.onResult("false");
                    return;
                }
                break;
            }

            case "fromUrl": {
                try {
                    photoBytes = Utils.toByteArray(photoUrl);
                } catch (IOException e) {
                    LOG.error("Error {} occured when reading URL {}", e.toString(), photo);
                    callback.onResult("false");
                    return;
                }
                break;
            }

            default: {
                LOG.error("Bad 'photo' string: path to file, URL or already uploaded 'photo()_()' was expected.");
                callback.onResult("false");
                return;
            }
        }

        if (photoBytes != null) {

            JSONObject params_getMessagesUploadServer = new JSONObject().put("peer_id", chatIdLong);
            api.call("photos.getMessagesUploadServer", params_getMessagesUploadServer, response -> {

                if (response.toString().equalsIgnoreCase("false")) {
                    LOG.error("Can't get messages upload server, aborting. Photo wont be attached to message.");
                    callback.onResult(false);
                    return;
                }

                String uploadUrl = new JSONObject(response.toString()).getString("upload_url");

                // if album_id == 3 make it negative
                uploadUrl = uploadUrl.replaceAll("aid=3", String.format("aid=%s", -new JSONObject(response.toString()).getInt("album_id")));

                String mimeType = "png";

                try {
                    InputStream is = new BufferedInputStream(new ByteArrayInputStream(photoBytes));
                    mimeType = URLConnection.guessContentTypeFromStream(is);
                    mimeType = mimeType.substring(mimeType.lastIndexOf('/')+1).replace("jpeg", "jpg");
                } catch (IOException e) {
                    LOG.error(e.getMessage());
                }

                // Uploading the photo
                MultipartUtility multipartUtility = new MultipartUtility(uploadUrl);
                multipartUtility.addBytesPart("photo", "photo."+mimeType, photoBytes);

                String response_uploadFileString = multipartUtility.finish();

                if (response_uploadFileString.length() < 2 || response_uploadFileString.contains("error") || !response_uploadFileString.contains("photo")) {
                    LOG.error("Photo wan't uploaded: {}", response_uploadFileString);
                    callback.onResult("false");
                    return;
                }

                JSONObject getPhotoStringResponse;

                try {
                    getPhotoStringResponse = new JSONObject(response_uploadFileString);
                } catch (JSONException ignored) {
                    LOG.error("Bad response of uploading photo: {}", response_uploadFileString);
                    callback.onResult("false");
                    return;
                }

                if (!getPhotoStringResponse.has("photo") || !getPhotoStringResponse.has("server") || !getPhotoStringResponse.has("hash")) {
                    LOG.error("Bad response of uploading photo, no 'photo', 'server' of 'hash' param: {}", getPhotoStringResponse.toString());
                    callback.onResult("false");
                    return;
                }

                String photoParam = getPhotoStringResponse.getString("photo");
                Object serverParam = getPhotoStringResponse.get("server");
                String hashParam = getPhotoStringResponse.getString("hash");

                JSONObject params_photosSaveMessagesPhoto = new JSONObject().put("photo", photoParam).put("server", serverParam + "").put("hash", hashParam);

                api.call("photos.saveMessagesPhoto", params_photosSaveMessagesPhoto, response1 -> {


                    if (response1.toString().equalsIgnoreCase("false")) {
                        LOG.error("Error when saving uploaded photo: response is 'false', see execution errors.");
                        callback.onResult("false");
                        return;
                    }

                    JSONObject response_saveMessagesPhoto = new JSONArray(response1.toString()).getJSONObject(0);

                    int ownerId = response_saveMessagesPhoto.getInt("owner_id"), id = response_saveMessagesPhoto.getInt("id");

                    String attach = String.format("photo%s_%s", ownerId, id);
                    callback.onResult(attach);
                });
            });
        }
    }

    /**
     * Async uploading doc
     * @param doc Doc link: url, from disk or already uploaded to VK as doc{owner_id}_{id}
     * @param callback callback
     */
    public void uploadDoc(JSONObject doc, Callback<Object> callback) {

        String type = null, fileNameField;
        File docFile = new File(doc.getString("doc"));
        if (docFile.exists()) {
            type = "fromFile";
        }

        URL docUrl = null;
        if (type == null) {
            try {
                docUrl = new URL(doc.getString("doc"));
                type = "fromUrl";
            } catch (MalformedURLException ignored) {
                LOG.error("Error when trying add doc to message: file not found, or url is bad. Your param: {}", doc);
                callback.onResult("false");
                return;
            }
        }

        byte[] docBytes;
        switch (type) {

            case "fromFile": {
                try {
                    docBytes = Files.readAllBytes(Paths.get(docFile.toURI()));
                    fileNameField = docFile.getName();
                } catch (IOException ignored) {
                    LOG.error("Error when reading file {}", docFile.getAbsolutePath());
                    callback.onResult("false");
                    return;
                }
                break;
            }

            case "fromUrl": {
                try {
                    URLConnection conn = docUrl.openConnection();

                    try {
                        docBytes = Utils.toByteArray(conn);
                        fileNameField = Utils.guessFileNameByContentType(conn.getContentType());
                    } finally {
                        Utils.close(conn);
                    }
                } catch (IOException ignored) {
                    LOG.error("Error when reading URL {}", doc);
                    callback.onResult("false");
                    return;
                }
                break;
            }

            default: {
                LOG.error("Bad file or url provided as doc: {}", doc);
                return;
            }
        }

        if (docBytes != null) {

            JSONObject params_getMessagesUploadServer = new JSONObject().put("peer_id", peerId).put("type", doc.getString("type"));
            api.call("docs.getMessagesUploadServer", params_getMessagesUploadServer, response -> {

                if (response.toString().equalsIgnoreCase("false")) {
                    LOG.error("Can't get messages upload server, aborting. Doc wont be attached to message.");
                    callback.onResult("false");
                    return;
                }

                String uploadUrl = new JSONObject(response.toString()).getString("upload_url");

                MultipartUtility multipartUtility = new MultipartUtility(uploadUrl);
                multipartUtility.addBytesPart("file", fileNameField, docBytes);
                String response_uploadFileString = multipartUtility.finish();

                if (response_uploadFileString.length() < 2 || response_uploadFileString.contains("error") || !response_uploadFileString.contains("file")) {
                    LOG.error("Doc won't uploaded: {}", response_uploadFileString);
                    callback.onResult("false");
                    return;
                }

                JSONObject getFileStringResponse;

                try {
                    getFileStringResponse = new JSONObject(response_uploadFileString);
                } catch (JSONException ignored) {
                    LOG.error("Bad response of uploading file: {}", response_uploadFileString);
                    callback.onResult("false");
                    return;
                }

                if (!getFileStringResponse.has("file")) {
                    LOG.error("Bad response of uploading doc, no 'file' param: {}", getFileStringResponse.toString());
                    callback.onResult("false");
                    return;
                }

                String fileParam = getFileStringResponse.getString("file");

                JSONObject params_photosSaveMessagesPhoto = new JSONObject().put("file", fileParam);

                api.call("docs.save", params_photosSaveMessagesPhoto, response1 -> {

                    if (response1.toString().equalsIgnoreCase("false")) {
                        LOG.error("Error when saving uploaded doc: response is 'false', see execution errors.");
                        callback.onResult("false");
                        return;
                    }

                    JSONObject response_saveMessagesPhotoe = new JSONArray(response1.toString()).getJSONObject(0);

                    int ownerId = response_saveMessagesPhotoe.getInt("owner_id"), id = response_saveMessagesPhotoe.getInt("id");

                    String attach = "doc" + ownerId + '_' + id;
                    callback.onResult(attach);
                });
            });
        }
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
        this.doc(doc, DocTypes.AUDIO_MESSAGE).send(callback);
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
            uploadPhoto(photo, response -> {
                if (!response.toString().equalsIgnoreCase("false")) {
                    this.attachments.addIfAbsent(response.toString());
                    send(callback);
                } else {
                    LOG.error("Some error occured when uploading photo.");
                }
            });
            return;
        }

        if (docsToUpload.size() > 0) {
            JSONObject doc = docsToUpload.get(0);
            docsToUpload.remove(0);
            uploadDoc(doc, response -> {
                if (!response.toString().equalsIgnoreCase("false")) {
                    this.attachments.addIfAbsent(response.toString());
                    send(callback);
                } else {
                    LOG.error("Some error occured when uploading doc.");
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
            JSONObject response = api.callSync("messages.getById", _client, "message_ids", getMessageId());

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
            JSONObject response = api.callSync("messages.getById", _client, "message_ids", getMessageId());

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

        JSONObject response = api.callSync("messages.getById", _client, "message_ids", getMessageId());

        if (response.has("response") && response.getJSONObject("response").getJSONArray("items").getJSONObject(0).has("attachments"))
            return response.getJSONObject("response").getJSONArray("items").getJSONObject(0).getJSONArray("attachments");

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

        for (int i = 0; i < attachments.length(); i++) {
            if (attachments.getJSONObject(i).has("type") && attachments.getJSONObject(i).getJSONObject(attachments.getJSONObject(i).getString("type")).has("type") && attachments.getJSONObject(i).getJSONObject(attachments.getJSONObject(i).getString("type")).getInt("type") == 3)
                return true;
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
                    if (key.startsWith("attach") && key.endsWith("type")) {

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
                '}';
    }
}
