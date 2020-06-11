package com.github.stormbit.sdk.clients;

import com.github.stormbit.sdk.callbacks.Callback;
import com.github.stormbit.sdk.utils.Utils;
import com.github.stormbit.sdk.utils.web.MultipartUtility;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by Storm-bit
 *
 * Group client, that contains important methods to work with groups
 */
public class Group extends Client {

    private String access_token;
    private Integer id;

    private static final Logger LOG = LoggerFactory.getLogger(Group.class);

    /**
     * Default constructor
     * @param access_token Access token key
     * @param id           Group id
     */
    public Group(String access_token, Integer id) {
        super(access_token, id);
        this.access_token = access_token;
        this.id = id;
    }

    /**
     * Upload group cover by file from url or from disk
     * @param cover cover
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
                LOG.error("Bad string was proviced to uploadCocver method: path to file or url was expected, but got this: {}, error: {}", cover, ignored.toString());
                return;
            }
        }

        updateCoverByFile(bytes, callback);
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