package com.github.stormbit.sdk.clients;

import com.github.stormbit.sdk.callbacks.Callback;
import com.github.stormbit.sdk.utils.Utils;
import com.github.stormbit.sdk.utils.web.MultipartUtility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by Storm-bit
 * <p>
 * Group client, that contains important methods to work with groups
 */
public class Group extends Client {

    private static final Logger LOG = LoggerFactory.getLogger(Group.class);

    /**
     * Default constructor
     *
     * @param access_token Access token key
     * @param id           Group id
     */
    public Group(String access_token, Integer id) {
        super(access_token, id);
    }

    /**
     * Upload group cover by file from url or from disk
     *
     * @param cover    cover
     * @param callback callback
     */
    public void uploadCover(String cover, Callback<Object> callback) {

        if (this.getId() == null || this.getId() == 0) {
            LOG.error("Please, provide group_id when initialising the client, because it's impossible to upload cover to group not knowing it id.");
            return;
        }

        byte[] bytes;

        File coverFile = new File(cover);
        if (coverFile.exists()) {
            try {
                bytes = Utils.toByteArray(coverFile.toURI().toURL());
            } catch (IOException ignored) {
                LOG.error("Cover file was exists, but IOException occured: {}", ignored.toString());
                return;
            }
        } else {
            URL coverUrl;
            try {
                coverUrl = new URL(cover);
                bytes = Utils.toByteArray(coverUrl);
            } catch (IOException ignored) {
                LOG.error("Bad string was provided to uploadCover method: path to file or url was expected, but got this: {}, error: {}", cover, ignored.toString());
                return;
            }
        }

        updateCoverByFile(bytes, callback);
    }

    private Object uploadPhotoToAlbum(String photo, int album_id) {
        if (this.getId() == null || this.getId() == 0) {
            LOG.error("Please, provide group_id when initialising the client, because it's impossible to upload cover to group not knowing it id.");
            return false;
        }

        byte[] bytes;

        File coverFile = new File(photo);
        if (coverFile.exists()) {
            try {
                bytes = Utils.toByteArray(coverFile.toURI().toURL());
            } catch (IOException ignored) {
                LOG.error("Cover file was exists, but IOException occurred: {}", ignored.toString());
                return false;
            }
        } else {
            URL coverUrl;
            try {
                coverUrl = new URL(photo);
                bytes = Utils.toByteArray(coverUrl);
            } catch (IOException ignored) {
                LOG.error("Bad string was provided to uploadPhotoToAlbum method: path to file or url was expected, but got this: {}, error: {}", photo, ignored.toString());
                return false;
            }
        }

        return uploadPhotoToAlbum(bytes, album_id);
    }

    public Object uploadPhotoToAlbum(byte[] photoBytes, int album_id) {
        if (photoBytes != null) {

            JSONObject params_getMessagesUploadServer = new JSONObject().put("album_id", album_id).put("group_id", getId());
            JSONObject response = this.api().callSync("photos.photos.getUploadServer", params_getMessagesUploadServer);

            if (response.toString().equalsIgnoreCase("false")) {
                LOG.error("Can't get messages upload server, aborting. Photo wont be attached to message.");
                return false;
            }

            String uploadUrl = new JSONObject(response.toString()).getString("upload_url");

            // if album_id == 3 make it negative
            uploadUrl = uploadUrl.replaceAll("aid=3", String.format("aid=%s", -new JSONObject(response.toString()).getInt("album_id")));

            String mimeType = "png";

            try {
                InputStream is = new BufferedInputStream(new ByteArrayInputStream(photoBytes));
                mimeType = URLConnection.guessContentTypeFromStream(is);
                mimeType = mimeType.substring(mimeType.lastIndexOf('/') + 1).replace("jpeg", "jpg");
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }

            // Uploading the photo
            MultipartUtility multipartUtility = new MultipartUtility(uploadUrl);
            multipartUtility.addBytesPart("photo", "photo." + mimeType, photoBytes);

            String response_uploadFileString = multipartUtility.finish();

            if (response_uploadFileString.length() < 2 || response_uploadFileString.contains("error") || !response_uploadFileString.contains("photo")) {
                LOG.error("Photo wan't uploaded: {}", response_uploadFileString);
                return false;
            }

            JSONObject getPhotoStringResponse;

            try {
                getPhotoStringResponse = new JSONObject(response_uploadFileString);
            } catch (JSONException ignored) {
                LOG.error("Bad response of uploading photo: {}", response_uploadFileString);
                return false;
            }

            if (!getPhotoStringResponse.has("photo") || !getPhotoStringResponse.has("server") || !getPhotoStringResponse.has("hash")) {
                LOG.error("Bad response of uploading photo, no 'photo', 'server' of 'hash' param: {}", getPhotoStringResponse.toString());
                return false;
            }

            String photoParam = getPhotoStringResponse.getString("photo");
            Object serverParam = getPhotoStringResponse.get("server");
            String hashParam = getPhotoStringResponse.getString("hash");

            JSONObject params_photosSaveMessagesPhoto = new JSONObject().put("photo", photoParam).put("server", serverParam + "").put("hash", hashParam);

            JSONObject response1 = this.api().callSync("photos.saveMessagesPhoto", params_photosSaveMessagesPhoto);


            if (response1.toString().equalsIgnoreCase("false")) {
                LOG.error("Error when saving uploaded photo: response is 'false', see execution errors.");
                return false;
            }

            JSONObject response_saveMessagesPhoto = new JSONArray(response1.toString()).getJSONObject(0);

            int ownerId = response_saveMessagesPhoto.getInt("owner_id"), id = response_saveMessagesPhoto.getInt("id");

            return String.format("photo%s_%s", ownerId, id);
        }

        return false;
    }

    /**
     * Updating cover by bytes (of file or url)
     *
     * @param bytes    bytes[]
     * @param callback response will return to callback
     */
    private void updateCoverByFile(byte[] bytes, Callback<Object>... callback) {

        JSONObject params_getUploadServer = new JSONObject()
                .put("group_id", getId())
                .put("crop_x", 0)
                .put("crop_y", 0)
                .put("crop_x2", 1590)
                .put("crop_y2", 400);

        api().call("photos.getOwnerCoverPhotoUploadServer", params_getUploadServer, response -> {

            String uploadUrl = new JSONObject(response.toString()).getString("upload_url");

            MultipartUtility multipartUtility = new MultipartUtility(uploadUrl);
            multipartUtility.addBytesPart("photo", "photo.png", bytes);
            String coverUploadedResponseString = multipartUtility.finish();

            coverUploadedResponseString = (coverUploadedResponseString != null && coverUploadedResponseString.length() > 2) ? coverUploadedResponseString : "{}";

            JSONObject coverUploadedResponse = new JSONObject(coverUploadedResponseString);

            if (coverUploadedResponse.has("hash") && coverUploadedResponse.has("photo")) {

                String hash_field = coverUploadedResponse.getString("hash");
                String photo_field = coverUploadedResponse.getString("photo");

                JSONObject params_saveCover = new JSONObject()
                        .put("hash", hash_field)
                        .put("photo", photo_field);

                boolean sync = true; // vk, please fix `execute` method!
                if (sync) {
                    JSONObject responseS = new JSONObject(api().callSync("photos.saveOwnerCoverPhoto", this, params_saveCover));
                    System.out.println("params is " + params_saveCover);
                    if (responseS.toString().length() < 10 || responseS.toString().contains("error")) {
                        LOG.error("Some error occured, cover not uploaded: {}", responseS);
                    }
                    if (callback.length > 0)
                        callback[0].onResult(responseS);
                } else {
                    api().call("photos.saveOwnerCoverPhoto", params_saveCover, response1 -> {

                        if (response1.toString().length() < 10 || response1.toString().contains("error")) {
                            LOG.error("Some error occured, cover not uploaded: {}", response1);
                        }
                        if (callback.length > 0) {
                            callback[0].onResult(response1);
                        }
                    });
                }
            } else {
                LOG.error("Error occured when uploading cover: no 'photo' or 'hash' param in response {}", coverUploadedResponse);
                if (callback.length > 0)
                    callback[0].onResult("false");
            }
        });
    }

    /* LongPoll API */

    public void onAudioNew(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("audio_new", callback);
    }

    public void onBoardPostDelete(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("board_post_delete", callback);
    }

    public void onBoardPostEdit(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("board_post_edit", callback);
    }

    public void onBoardPostNew(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("board_post_new", callback);
    }

    public void onBoardPostRestore(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("board_post_restore", callback);
    }

    public void onGroupChangePhoto(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("group_change_photo", callback);
    }

    public void onGroupChangeSettings(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("group_change_settings", callback);
    }

    public void onGroupJoin(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("group_join", callback);
    }

    public void onGroupLeave(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("group_leave", callback);
    }

    public void onGroupOfficersEdit(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("group_officers_edit", callback);
    }

    public void onPollVoteNew(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("poll_vote_new", callback);
    }

    public void onMarketCommentDelete(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("market_comment_delete", callback);
    }

    public void onMarketCommentEdit(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("market_comment_edit", callback);
    }

    public void onMarketCommentNew(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("market_comment_new", callback);
    }

    public void onMarketCommentRestore(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("market_comment_restore", callback);
    }

    public void onMessageAllow(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("message_allow", callback);
    }

    public void onMessageDeny(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("message_deny", callback);
    }

    public void onMessageNew(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("message_new", callback);
    }

    public void onMessageReply(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("message_reply", callback);
    }

    public void onPhotoCommentEdit(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("photo_comment_edit", callback);
    }

    public void onPhotoCommentNew(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("photo_comment_new", callback);
    }

    public void onPhotoCommentRestore(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("photo_comment_restore", callback);
    }

    public void onPhotoNew(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("photo_new", callback);
    }

    public void onPhotoCommentDelete(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("photo_comment_delete", callback);
    }

    public void onVideoCommentEdit(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("video_comment_edit", callback);
    }

    public void onVideoCommentNew(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("video_comment_new", callback);
    }

    public void onVideoCommentRestore(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("video_comment_restore", callback);
    }

    public void onVideoNew(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("video_new", callback);
    }

    public void onVideoCommentDelete(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("video_comment_delete", callback);
    }

    public void onWallPostNew(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("wall_post_new", callback);
    }

    public void onWallReplyDelete(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("wall_reply_delete", callback);
    }

    public void onWallReplyEdit(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("wall_reply_edit", callback);
    }

    public void onWallReplyNew(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("wall_reply_new", callback);
    }

    public void onWallReplyRestore(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("wall_reply_restore", callback);
    }

    public void onWallRepost(Callback<JSONObject> callback) {
        this.longPoll().registerCallback("wall_repost", callback);
    }
}