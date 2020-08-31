package com.github.stormbit.sdk.clients;

import com.github.stormbit.sdk.callbacks.Callback;
import org.json.JSONObject;

/**
 * Created by Storm-bit
 *
 * Group client, that contains important methods to work with groups
 */
@SuppressWarnings("unused")
public class Group extends Client {

    /**
     * Default constructor
     *
     * @param access_token Access token key
     * @param id           Group id
     */
    public Group(String access_token, Integer id) {
        super(access_token, id);
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