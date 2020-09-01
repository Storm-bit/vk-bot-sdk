package com.github.stormbit.sdk.utils.vkapi;

import com.github.stormbit.sdk.callbacks.Callback;
import com.github.stormbit.sdk.clients.Client;
import com.github.stormbit.sdk.utils.Utils;
import com.github.stormbit.sdk.utils.vkapi.docs.DocTypes;
import net.dongliu.requests.Requests;
import net.dongliu.requests.body.Part;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;

@SuppressWarnings("unused")
public class Upload {
    private final Logger log = LoggerFactory.getLogger(Upload.class);
    private final API api;

    public Upload(Client client) {
        api = client.api();
    }

    /* Async methods */

    /**
     * @param photo String URL, link to vk photo or path to file
     * @param peerId peer id
     * @param callback callback
     */
    public void uploadPhotoAsync(String photo, int peerId, Callback<Object> callback) {
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
                log.error("Error when trying add photo to message: file not found, or url is bad. Your param: {}", photo);
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
                    log.error("Error when reading file {}", photoFile.getAbsolutePath());
                    callback.onResult("false");
                    return;
                }

                break;
            }

            case "fromUrl": {
                try {
                    photoBytes = Utils.toByteArray(photoUrl);
                } catch (IOException e) {
                    log.error("Error {} occurred when reading URL {}", e.toString(), photo);
                    callback.onResult("false");
                    return;
                }

                break;
            }

            default: {
                log.error("Bad 'photo' string: path to file, URL or already uploaded 'photo()_()' was expected.");
                callback.onResult("false");
                return;
            }
        }

        if (photoBytes != null) {
            uploadPhotoAsync(photoBytes, peerId, callback);
        }
    }

    /**
     * Async uploading photos
     * @param photoBytes Photo bytes
     * @param peerId peer id
     * @param callback callback
     */
    public void uploadPhotoAsync(byte[] photoBytes, int peerId, Callback<Object> callback) {

        if (photoBytes != null) {

            JSONObject params_getMessagesUploadServer = new JSONObject().put("peer_id", peerId);
            api.call("photos.getMessagesUploadServer", params_getMessagesUploadServer, response -> {

                if (response.toString().equalsIgnoreCase("false")) {
                    log.error("Can't get messages upload server, aborting. Photo wont be attached to message.");
                    callback.onResult("false");
                    return;
                }

                String uploadUrl = new JSONObject(response.toString()).getString("upload_url");

                String mimeType;

                try {
                    mimeType = Utils.getMimeType(photoBytes);
                } catch (IOException e) {
                    log.error(e.getMessage());

                    callback.onResult("false");
                    return;
                }

                String response_uploadFileString = Requests
                        .post(uploadUrl)
                        .multiPartBody(Part.file("photo", "image."+mimeType, photoBytes))
                        .send().readToText();

                if (response_uploadFileString.length() < 2 || response_uploadFileString.contains("error") || !response_uploadFileString.contains("photo")) {
                    log.error("Photo wan't uploaded: {}", response_uploadFileString);
                    callback.onResult("false");
                    return;
                }

                JSONObject getPhotoStringResponse;

                try {
                    getPhotoStringResponse = new JSONObject(response_uploadFileString);
                } catch (JSONException ignored) {
                    log.error("Bad response of uploading photo: {}", response_uploadFileString);
                    callback.onResult("false");
                    return;
                }

                if (!getPhotoStringResponse.has("photo") || !getPhotoStringResponse.has("server") || !getPhotoStringResponse.has("hash")) {
                    log.error("Bad response of uploading photo, no 'photo', 'server' of 'hash' param: {}", getPhotoStringResponse.toString());
                    callback.onResult("false");
                    return;
                }

                var photoParam = getPhotoStringResponse.getString("photo");
                var serverParam = getPhotoStringResponse.get("server");
                var hashParam = getPhotoStringResponse.getString("hash");

                JSONObject params_photosSaveMessagesPhoto = new JSONObject().put("photo", photoParam).put("server", serverParam).put("hash", hashParam);

                api.call("photos.saveMessagesPhoto", params_photosSaveMessagesPhoto, response1 -> {


                    if (response1.toString().equalsIgnoreCase("false")) {
                        log.error("Error when saving uploaded photo: response is 'false', see execution errors.");
                        callback.onResult("false");
                        return;
                    }

                    JSONObject response_saveMessagesPhoto = new JSONArray(response1.toString()).getJSONObject(0);

                    int ownerId = response_saveMessagesPhoto.getInt("owner_id");
                    int id = response_saveMessagesPhoto.getInt("id");

                    String attach = String.format("photo%s_%s", ownerId, id);

                    callback.onResult(attach);
                });
            });
        }
    }

    /**
     * Async uploading doc
     * @param doc Doc link: url, from disk or already uploaded to VK as doc{owner_id}_{id}
     * @param peerId peer id
     * @param callback callback
     */
    public void uploadDocAsync(JSONObject doc, int peerId, Callback<Object> callback) {
        String type = null;
        String fileNameField;

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
                log.error("Error when trying add doc to message: file not found, or url is bad. Your param: {}", doc);
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
                    log.error("Error when reading file {}", docFile.getAbsolutePath());
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
                    log.error("Error when reading URL {}", doc);
                    callback.onResult("false");
                    return;
                }

                break;
            }

            default: {
                log.error("Bad file or url provided as doc: {}", doc);
                return;
            }
        }

        if (docBytes != null) {
            JSONObject params = new JSONObject().put("peer_id", peerId).put("type", doc.getString("type"));

            api.call("docs.getMessagesUploadServer", params, response -> {
                if (response.toString().equalsIgnoreCase("false")) {
                    log.error("Can't get messages upload server, aborting. Doc wont be attached to message.");
                    callback.onResult("false");
                    return;
                }

                String uploadUrl = new JSONObject(response.toString()).getString("upload_url");

                String response_uploadFileString = Requests
                        .post(uploadUrl)
                        .multiPartBody(Part.file("file", fileNameField, docBytes))
                        .send().readToText();

                if (response_uploadFileString.length() < 2 || response_uploadFileString.contains("error") || !response_uploadFileString.contains("file")) {
                    log.error("Doc won't uploaded: {}", response_uploadFileString);
                    callback.onResult("false");
                    return;
                }

                JSONObject getFileStringResponse;

                try {
                    getFileStringResponse = new JSONObject(response_uploadFileString);
                } catch (JSONException ignored) {
                    log.error("Bad response of uploading file: {}", response_uploadFileString);
                    callback.onResult("false");

                    return;
                }

                if (!getFileStringResponse.has("file")) {
                    log.error("Bad response of uploading doc, no 'file' param: {}", getFileStringResponse.toString());
                    callback.onResult("false");

                    return;
                }

                String fileParam = getFileStringResponse.getString("file");
                JSONObject params_photosSaveMessagesPhoto = new JSONObject().put("file", fileParam);

                api.call("docs.save", params_photosSaveMessagesPhoto, response1 -> {
                    if (response1.toString().equalsIgnoreCase("false")) {
                        log.error("Error when saving uploaded doc: response is 'false', see execution errors.");
                        callback.onResult("false");

                        return;
                    }

                    JSONObject response_saveMessagesPhotoe = new JSONArray(response1.toString()).getJSONObject(0);

                    int ownerId = response_saveMessagesPhotoe.getInt("owner_id");
                    int id = response_saveMessagesPhotoe.getInt("id");
                    String attach = "doc" + ownerId + '_' + id;

                    callback.onResult(attach);
                });
            });
        }
    }

    /**
     *
     * @param photo String URL, link to vk photo or path to file
     * @param chatId chat id
     * @param callback callback
     */
    public void uploadPhotoChatAsync(String photo, int chatId, Callback<Object> callback) {
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
                log.error("Error when trying add photo to message: file not found, or url is bad. Your param: {}", photo);
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
                    log.error("Error when reading file {}", photoFile.getAbsolutePath());
                    callback.onResult("false");
                    return;
                }

                break;
            }

            case "fromUrl": {
                try {
                    photoBytes = Utils.toByteArray(photoUrl);
                } catch (IOException e) {
                    log.error("Error {} occurred when reading URL {}", e.toString(), photo);
                    callback.onResult("false");
                    return;
                }

                break;
            }

            default: {
                log.error("Bad 'photo' string: path to file, URL or already uploaded 'photo()_()' was expected.");
                callback.onResult("false");
                return;
            }
        }

        uploadPhotoChatAsync(photoBytes, chatId, callback);
    }

    /**
     * @param photoBytes bytes
     * @param chatId chat id
     * @param callback callback
     */
    public void uploadPhotoChatAsync(byte[] photoBytes, int chatId, Callback<Object> callback) {
        if (photoBytes != null) {
            JSONObject params_getMessagesUploadServer = new  JSONObject().put("chat_id", chatId);

            api.call("photos.getChatUploadServer", params_getMessagesUploadServer, response -> {
                if (response.toString().equalsIgnoreCase("false")) {
                    log.error("Can't get messages upload server, aborting. Photo wont be attached to message.");
                    callback.onResult(false);
                    return;
                }

                String uploadUrl = new JSONObject(response.toString()).getString("upload_url");

                String mimeType;

                try {
                    mimeType = Utils.getMimeType(photoBytes);
                } catch (IOException e) {
                    log.error(e.getMessage());
                    callback.onResult("false");
                    return;
                }

                String responseUploadFileString = Requests
                        .post(uploadUrl)
                        .multiPartBody(Part.file("file", "photo."+mimeType, photoBytes))
                        .send().readToText();

                if (responseUploadFileString.length() < 2 || responseUploadFileString.contains("error") || !responseUploadFileString.contains("response")) {
                    log.error("Photo wan't uploaded: {}", responseUploadFileString);
                    callback.onResult("false");
                    return;
                }

                JSONObject getPhotoStringResponse;

                try {
                    getPhotoStringResponse = new JSONObject(responseUploadFileString);
                } catch (JSONException ignored) {
                    log.error("Bad response of uploading photo: {}", responseUploadFileString);
                    callback.onResult("false");
                    return;
                }

                if (!getPhotoStringResponse.has("response")) {
                    log.error("Bad response of uploading chat photo, no 'response' param: {}", getPhotoStringResponse.toString());
                    callback.onResult("false");
                    return;
                }

                String responseParam = getPhotoStringResponse.getString("response");
                JSONObject params_photosSaveMessagesPhoto = new JSONObject().put("file", responseParam);

                api.call("messages.setChatPhoto", params_photosSaveMessagesPhoto, response1 -> {
                    if (response1.toString().equalsIgnoreCase("false")) {
                        log.error("Error when saving uploaded photo: response is 'false', see execution errors.");
                        callback.onResult("false");
                        return;
                    }

                    callback.onResult(response1);
                });
            });
        }
    }

    /**
     * Upload group cover by file from url or from disk
     *
     * @param cover    cover
     * @param groupId group id
     * @param callback callback
     */
    public void uploadCoverGroupAsync(String cover, int groupId, Callback<Object> callback) {
        if (groupId == 0) {
            log.error("Please, provide group_id when initialising the client, because it's impossible to upload cover to group not knowing it id.");
            return;
        }

        byte[] bytes;
        File coverFile = new File(cover);

        if (coverFile.exists()) {
            try {
                bytes = Utils.toByteArray(coverFile.toURI().toURL());
            } catch (IOException e) {
                log.error("Cover file was exists, but IOException occurred: {}", e.toString());
                return;
            }
        } else {
            URL coverUrl;
            try {
                coverUrl = new URL(cover);
                bytes = Utils.toByteArray(coverUrl);
            } catch (IOException e) {
                log.error("Bad string was provided to uploadCover method: path to file or url was expected, but got this: {}, error: {}", cover, e.toString());
                return;
            }
        }

        uploadCoverGroupAsync(bytes, groupId, callback);
    }


    /**
     * Updating cover by bytes (of file or url)
     *
     * @param bytes    bytes[]
     * @param groupId  group id
     * @param callback response will return to callback
     */
    public void uploadCoverGroupAsync(byte[] bytes, int groupId, Callback<Object> callback) {
        JSONObject params_getUploadServer = new JSONObject()
                .put("group_id", groupId)
                .put("crop_x", 0)
                .put("crop_y", 0)
                .put("crop_x2", 1590)
                .put("crop_y2", 400);

        api.call("photos.getOwnerCoverPhotoUploadServer", params_getUploadServer, response -> {
            String uploadUrl = new JSONObject(response.toString()).getString("upload_url");

            String mimeType;

            try {
                mimeType = Utils.getMimeType(bytes);
            } catch (IOException e) {
                log.error(e.getMessage());
                if (callback != null) callback.onResult("false");
                return;
            }

            String coverUploadedResponseString = Requests
                    .post(uploadUrl)
                    .multiPartBody(Part.file("photo", "image."+mimeType, bytes))
                    .send().readToText();

            coverUploadedResponseString = (coverUploadedResponseString != null && coverUploadedResponseString.length() > 2) ? coverUploadedResponseString : "{}";

            JSONObject coverUploadedResponse = new JSONObject(coverUploadedResponseString);

            if (coverUploadedResponse.has("hash") && coverUploadedResponse.has("photo")) {
                String hash_field = coverUploadedResponse.getString("hash");
                String photo_field = coverUploadedResponse.getString("photo");

                JSONObject params_saveCover = new JSONObject()
                        .put("hash", hash_field)
                        .put("photo", photo_field);

                JSONObject responseS = new JSONObject(api.callSync("photos.saveOwnerCoverPhoto", params_saveCover));

                if (responseS.toString().length() < 10 || responseS.toString().contains("error")) {
                    log.error("Some error occurred, cover not uploaded: {}", responseS);
                }

                if (callback != null) callback.onResult(responseS);
            } else {
                log.error("Error occurred when uploading cover: no 'photo' or 'hash' param in response {}", coverUploadedResponse);

                if (callback != null) callback.onResult("false");
            }
        });
    }

    /* Sync */

    /**
     * @param photo String URL, link to vk photo or path to file
     * @param group_id group id
     * @param album_id album id
     * @return attachment
     */
    public String uploadPhotoToAlbum(String photo, int group_id, int album_id) {
        if (group_id == 0) {
            log.error("Please, provide group_id when initialising the client, because it's impossible to upload cover to group not knowing it id.");
            return null;
        }

        byte[] bytes;
        File coverFile = new File(photo);

        if (coverFile.exists()) {
            try {
                bytes = Utils.toByteArray(coverFile.toURI().toURL());
            } catch (IOException e) {
                log.error("Cover file was exists, but IOException occurred: {}", e.toString());
                return null;
            }

        } else {
            URL coverUrl;
            try {
                coverUrl = new URL(photo);
                bytes = Utils.toByteArray(coverUrl);
            } catch (IOException e) {
                log.error("Bad string was provided to uploadPhotoToAlbum method: path to file or url was expected, but got this: {}, error: {}", photo, e.toString());
                return null;
            }
        }

        return uploadPhotoToAlbum(bytes, group_id, album_id);
    }

    /**
     * @param photoBytes bytes
     * @param album_id album id
     * @param group_id group id
     * @return attachment
     */

    public String uploadPhotoToAlbum(byte[] photoBytes, int group_id, int album_id) {
        if (photoBytes != null) {
            JSONObject params_getMessagesUploadServer = new JSONObject().put("album_id", album_id);

            if (group_id != 0) {
                params_getMessagesUploadServer.put("group_id", group_id);
            }

            JSONObject response = api.callSync("photos.getUploadServer", params_getMessagesUploadServer);

            if (response.toString().equalsIgnoreCase("false")) {
                log.error("Can't get messages upload server, aborting. Photo wont be attached to message.");
                return null;
            }

            String uploadUrl = new JSONObject(response.toString()).getString("upload_url");

            String mimeType;

            try {
                mimeType = Utils.getMimeType(photoBytes);
            } catch (IOException e) {
                log.error(e.getMessage());
                return null;
            }

            // Uploading the photo
            String response_uploadFileString = Requests
                    .post(uploadUrl)
                    .multiPartBody(Part.file("photo", "image."+mimeType, photoBytes))
                    .send().readToText();

            if (response_uploadFileString.length() < 2 || response_uploadFileString.contains("error") || !response_uploadFileString.contains("photo")) {
                log.error("Photo wan't uploaded: {}", response_uploadFileString);
                return null;
            }

            JSONObject getPhotoStringResponse;

            try {
                getPhotoStringResponse = new JSONObject(response_uploadFileString);
            } catch (JSONException ignored) {
                log.error("Bad response of uploading photo: {}", response_uploadFileString);
                return null;
            }

            if (!getPhotoStringResponse.has("photo") || !getPhotoStringResponse.has("server") || !getPhotoStringResponse.has("hash")) {
                log.error("Bad response of uploading photo, no 'photo', 'server' of 'hash' param: {}", getPhotoStringResponse.toString());
                return null;
            }

            String photoParam = getPhotoStringResponse.getString("photo");
            Object serverParam = getPhotoStringResponse.get("server");
            String hashParam = getPhotoStringResponse.getString("hash");
            JSONObject params_photosSaveMessagesPhoto = new JSONObject().put("photo", photoParam).put("server", serverParam.toString() + "").put("hash", hashParam);

            JSONObject response1 = api.callSync("photos.saveMessagesPhoto", params_photosSaveMessagesPhoto);

            if (response1.toString().equalsIgnoreCase("false")) {
                log.error("Error when saving uploaded photo: response is 'false', see execution errors.");
                return null;
            }

            JSONObject response_saveMessagesPhoto = new JSONArray(response1.toString()).getJSONObject(0);
            int ownerId = response_saveMessagesPhoto.getInt("owner_id");
            int id = response_saveMessagesPhoto.getInt("id");

            return String.format("photo%s_%s", ownerId, id);
        }

        return null;
    }

    /**
     * @param photo String URL, link to vk photo or path to file
     * @param peerId peer id
     * @return attachment
     */
    public String uploadPhoto(String photo, int peerId) {
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
            } catch (MalformedURLException e) {
                log.error("Error when trying add photo to message: file not found, or url is bad. Your param: {}", photo);
                return null;
            }
        }

        byte[] photoBytes;

        switch (type) {
            case "fromFile": {
                try {
                    photoBytes = Files.readAllBytes(Paths.get(photoFile.toURI()));
                } catch (IOException ignored) {
                    log.error("Error when reading file {}", photoFile.getAbsolutePath());
                    return null;
                }

                break;
            }

            case "fromUrl": {
                try {
                    photoBytes = Utils.toByteArray(photoUrl);
                } catch (IOException e) {
                    log.error("Error {} occurred when reading URL {}", e.toString(), photo);
                    return null;
                }

                break;
            }

            default: {
                log.error("Bad 'photo' string: path to file, URL or already uploaded 'photo()_()' was expected.");
                return null;
            }
        }

        return uploadPhoto(photoBytes, peerId);
    }

    /**
     * Synchronous adding photo to the message
     *
     * @param photoBytes photo bytes
     * @param peerId peer id
     * @return attachment
     */
    public String uploadPhoto(byte[] photoBytes, int peerId) {
        if (photoBytes != null) {

            // Getting of server for uploading the photo
            JSONObject getUploadServerResponse = api.callSync("photos.getMessagesUploadServer", "peer_id", peerId);
            var uploadUrl = (getUploadServerResponse.has("response")) ? (getUploadServerResponse.getJSONObject("response").has("upload_url")) ? getUploadServerResponse.getJSONObject("response").getString("upload_url") : null : null;

            // Some error
            if (uploadUrl == null) {
                log.error("No upload url in response: {}", getUploadServerResponse);
                return null;
            }

            String mimeType;

            try {
                mimeType = Utils.getMimeType(photoBytes);
            } catch (IOException e) {
                log.error(e.getMessage());
                return null;
            }

            // Uploading the photo
            String uploadingOfPhotoResponseString = Requests
                    .post(uploadUrl)
                    .multiPartBody(Part.file("photo", "photo."+mimeType, photoBytes))
                    .send().readToText();

            JSONObject uploadingOfPhotoResponse;

            try {
                uploadingOfPhotoResponse = new JSONObject(uploadingOfPhotoResponseString);
            } catch (JSONException e) {
                log.error("Bad response of uploading photo: {}, error: {}", uploadingOfPhotoResponseString, e.toString());
                return null;
            }

            // Getting necessary params
            String server;
            String photo_param;
            String hash;

            if (uploadingOfPhotoResponse.has("server") && uploadingOfPhotoResponse.has("photo") && uploadingOfPhotoResponse.has("hash")) {
                server = "" + uploadingOfPhotoResponse.getInt("server");
                photo_param = uploadingOfPhotoResponse.get("photo").toString();
                hash = uploadingOfPhotoResponse.getString("hash");
            } else {
                log.error("No 'photo', 'server' or 'hash' param in response {}", uploadingOfPhotoResponseString);
                return null;
            }

            // Saving the photo
            JSONObject saveMessagesPhotoResponse = api.callSync("photos.saveMessagesPhoto", "server", server, "photo", photo_param, "hash", hash);

            return (saveMessagesPhotoResponse.has("response")) ? "photo" + saveMessagesPhotoResponse.getJSONArray("response").getJSONObject(0).getInt("owner_id") + "_" + saveMessagesPhotoResponse.getJSONArray("response").getJSONObject(0).getInt("id") : "";
        }

        return null;
    }

    /**
     * Synchronous uploading doc
     * @param doc       String URL, link to vk doc or path to file
     * @param peerId peer id
     * @param typeOfDoc Type of doc, 'audio_message' or 'graffiti' ('doc' as default)
     * @return attachment
     */
    public String uploadDoc(String doc, int peerId, DocTypes typeOfDoc) {
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
                log.error("Error when trying add doc to message: file not found, or url is bad. Your param: {}", doc);
                return null;
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
                    log.error("Error when reading file {}", docFile.getAbsolutePath());
                    return null;
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
                    log.error("Error {} occurred when reading URL {}", e.toString(), doc);
                    return null;
                }

                break;
            }

            default: {
                log.error("Bad 'doc' string: path to file, URL or already uploaded 'doc()_()' was expected, but got this: {}", doc);
                return null;
            }
        }

        return uploadDoc(docBytes, peerId, typeOfDoc, fileNameField);
    }

    /**
     * @param docBytes bytes
     * @param peerId peer id
     * @param typeOfDoc Type of doc, 'audio_message' or 'graffiti' ('doc' as default)
     * @param fileNameField file name field
     * @return attachment
     */

    public String uploadDoc(byte[] docBytes, int peerId, DocTypes typeOfDoc, String fileNameField) {
        if (docBytes != null) {

            // Getting of server for uploading the photo
            JSONObject getUploadServerResponse = api.callSync("docs.getMessagesUploadServer", "peer_id", peerId, "type", typeOfDoc.getType());
            String uploadUrl = (getUploadServerResponse.has("response")) ? (getUploadServerResponse.getJSONObject("response").has("upload_url")) ? getUploadServerResponse.getJSONObject("response").getString("upload_url") : null : null;

            // Some error
            if (uploadUrl == null) {
                log.error("No upload url in response: {}", getUploadServerResponse);
                return null;
            }

            // Uploading the photo
            String uploadingOfDocResponseString = Requests
                    .post(uploadUrl)
                    .multiPartBody(Part.file("file", fileNameField, docBytes))
                    .send().readToText();

            JSONObject uploadingOfDocResponse;

            try {
                uploadingOfDocResponse = new JSONObject(uploadingOfDocResponseString);
            } catch (JSONException e) {
                log.error("Bad response of uploading doc: {}, error: {}", uploadingOfDocResponseString, e.toString());
                return null;
            }

            // Getting necessary params
            String file;

            if (uploadingOfDocResponse.has("file")) {
                file = uploadingOfDocResponse.getString("file");
            } else {
                log.error("No 'file' param in response {}", uploadingOfDocResponseString);

                return null;
            }

            // Saving the photo
            JSONObject saveMessagesDocResponse = api.callSync("docs.save", "file", file);

            return (saveMessagesDocResponse.has("response")) ? "doc" + saveMessagesDocResponse.getJSONArray("response").getJSONObject(0).getInt("owner_id") + "_" + saveMessagesDocResponse.getJSONArray("response").getJSONObject(0).getInt("id") : "";
        } else {
            log.error("Got file or url of doc to be uploaded, but some error occured and readed 0 bytes.");
        }

        return null;
    }
}
