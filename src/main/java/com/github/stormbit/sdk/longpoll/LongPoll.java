package com.github.stormbit.sdk.longpoll;

import com.github.stormbit.sdk.callbacks.AbstractCallback;
import com.github.stormbit.sdk.callbacks.Callback;
import com.github.stormbit.sdk.clients.Client;
import com.github.stormbit.sdk.longpoll.responses.GetLongPollServerResponse;
import com.github.stormbit.sdk.utils.Utils;
import com.github.stormbit.sdk.utils.web.Connection;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by PeterSamokhin on 28/09/2017 21:59
 * Updated by Storm-bit on 03/04/2020 19:40
 *
 * Class for work with VK longpoll server
 * More: <a href="https://vk.com/dev/using_longpoll">link</a>
 */
public class LongPoll {

    private static final Logger LOG = LoggerFactory.getLogger(LongPoll.class);

    private String server = null;
    private String key = null;
    private Integer ts = null;
    public Integer pts = null;

    private Integer wait = 25;

    /**
     * 2 + 32 + 128
     * attachments + pts + random_id
     */
    private Integer mode = 162;

    private Integer version = 3;
    private Integer need_pts = 1;
    private Double API = 5.101;

    private volatile boolean longpollIsOn = false;

    private final UpdatesHandler updatesHandler;
    private final Client client;

    /**
     * If true, all updates from longpoll server
     * will be logged to level 'INFO'
     */
    private volatile boolean logUpdates = false;

    /**
     * Simple default constructor that requires only access token
     *
     * @param client client with your access token key, more: <a href="https://vk.com/dev/access_token">link</a>
     */
    public LongPoll(Client client) {
        if (client.token == null) {
            this.updatesHandler = new UpdatesHandler1(client);
        } else {
            this.updatesHandler = new UpdatesHandler2(client);
        }
        this.updatesHandler.start();
        this.client = client;

        boolean dataSetted = setData(null, null, null, null, null);

        while (!dataSetted) {
            LOG.error("Some error occurred when trying to get longpoll settings, aborting. Trying again in 1 sec.");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            dataSetted = setData(null, null, null, null, null);
        }

        if (!longpollIsOn) {
            longpollIsOn = true;
            Thread threadLongpollListener = new Thread(this::startListening);
            threadLongpollListener.setName("threadLongpollListener");
            threadLongpollListener.start();
        }
    }

    /**
     * Custom constructor
     * @param client   client with your access token key, more: <a href="https://vk.com/dev/access_token">link</a>
     * @param need_pts more: <a href="https://vk.com/dev/using_longpoll">link</a>
     * @param version  more: <a href="https://vk.com/dev/using_longpoll">link</a>
     * @param API      more: <a href="https://vk.com/dev/using_longpoll">link</a>
     * @param wait     more: <a href="https://vk.com/dev/using_longpoll">link</a>
     * @param mode     more: <a href="https://vk.com/dev/using_longpoll">link</a>
     */
    public LongPoll(Client client, Integer need_pts, Integer version, Double API, Integer wait, Integer mode) {

        if (client.token == null) {
            this.updatesHandler = new UpdatesHandler1(client);
        } else {
            this.updatesHandler = new UpdatesHandler2(client);
        }
        this.updatesHandler.start();
        this.client = client;

        boolean dataSetted = setData(need_pts, version, API, wait, mode);

        while (!dataSetted) {
            LOG.error("Some error occured when trying to get longpoll settings, aborting. Trying again in 1 sec.");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            dataSetted = setData(need_pts, version, API, wait, mode);
        }

        if (!longpollIsOn) {
            longpollIsOn = true;
            Thread threadLongpollListener = new Thread(this::startListening);
            threadLongpollListener.setName("threadLongpollListener");
            threadLongpollListener.start();
        }
    }

    /**
     * If you need to set new longpoll server, or restart listening
     * off old before.
     */
    public void off() {
        longpollIsOn = false;
    }

    /**
     * Add callback to the map
     *
     * @param name     Callback name
     * @param callback Callback
     */
    public void registerCallback(String name, Callback callback) {
        updatesHandler.registerCallback(name, callback);
    }

    /**
     * Add callback to the map
     *
     * @param name     Callback name
     * @param callback Callback
     */
    public void registerAbstractCallback(String name, AbstractCallback callback) {
        updatesHandler.registerAbstractCallback(name, callback);
    }

    /**
     * Add callback to the map
     *
     * @param name     Callback name
     * @param callback Callback
     */
    public void registerChatCallback(String name, AbstractCallback callback) {
        updatesHandler.registerChatCallback(name, callback);
    }

    /**
     * Setting all necessary parameters
     *
     * @param need_pts param, info: <a href="https://vk.com/dev/using_longpoll">link</a>
     * @param version  param, info: <a href="https://vk.com/dev/using_longpoll">link</a>
     * @param API      param, info: <a href="https://vk.com/dev/using_longpoll">link</a>
     * @param wait     param, info: <a href="https://vk.com/dev/using_longpoll">link</a>
     * @param mode     param, info: <a href="https://vk.com/dev/using_longpoll">link</a>
     */
    private boolean setData(Integer need_pts, Integer version, Double API, Integer wait, Integer mode) {

        this.need_pts = need_pts == null ? this.need_pts : need_pts;
        this.version = version == null ? this.version : version;
        this.API = API == null ? this.API : API;
        this.wait = wait == null ? this.wait : wait;
        this.mode = mode == null ? this.mode : mode;

        GetLongPollServerResponse serverResponse;

        if (client.token == null) {
            serverResponse = getLongPollServer();
        } else {
            serverResponse = getLongPollServerGroup();
        }

        if (serverResponse == null) {
            LOG.error("Some error occurred, bad response returned from getting LongPoll server settings (server, key, ts, pts).");
            return false;
        }

        String serv = serverResponse.getServer();

        if (!serv.startsWith("https://")) {
            serv = "https://" + serv;
        }
        this.server = serv;
        this.key = serverResponse.getKey();
        this.ts = serverResponse.getTs();
        this.pts = serverResponse.getPts();

        Utils.longpollServer = server + "?act=a_check&key=" + key + "&ts=" + ts + "&wait=" + wait + "&mode=" + mode + "&version=" + version + "&msgs_limit=100000";

        return true;
    }

    private GetLongPollServerResponse getLongPollServer() {
        String method = "messages.getLongPollServer";

        if (!Utils._hashes.has(method)) {
            Utils.get_hash(client.auth(), method);
        }

        JSONObject data_ = new JSONObject();

        JSONObject params = new JSONObject();
        params.put("need_pts", need_pts);
        params.put("lp_version", version);

        for (String key : params.keySet()) {
            data_.put(key, params.get(key));
        }

        Map<String, Object> prms = new HashMap<>();
        for (String key : data_.keySet()) {
            prms.put(key, data_.get(key));
        }

        JSONObject result = client.api().callSync(method, prms);

        JSONObject response;

        if (!result.has("response") || !result.getJSONObject("response").has("key") || !result.getJSONObject("response").has("server") || !result.getJSONObject("response").has("ts")) {
            LOG.error("Bad response of getting longpoll server!\nQuery: {}\n Response: {}", data_.toString(), result);
            return null;
        }

        try {
            response = result.getJSONObject("response");
        } catch (JSONException e) {
            LOG.error("Bad response of getting longpoll server.");
            return null;
        }

        LOG.info("GetLongPollServerResponse: \n{}\n", response);

        return new GetLongPollServerResponse(
                response.getString("key"),
                response.getString("server"),
                response.getInt("ts"),
                response.getInt("pts")
        );
    }

    private GetLongPollServerResponse getLongPollServerGroup() {
        String method = "groups.getLongPollServer";

        JSONObject params = new JSONObject();
        params.put("group_id", client.getId());

        Map<String, Object> prms = new HashMap<>();
        for (String key : params.keySet()) {
            prms.put(key, params.get(key));
        }

        JSONObject result = client.api().callSync(method, params);

        JSONObject response;

        if (!result.has("response") || !result.getJSONObject("response").has("key") || !result.getJSONObject("response").has("server") || !result.getJSONObject("response").has("ts")) {
            LOG.error("Bad response of getting longpoll server!\nQuery: {}\n Response: {}", params.toString(), result);
            return null;
        }

        try {
            response = result.getJSONObject("response");
        } catch (JSONException e) {
            LOG.error("Bad response of getting longpoll server.");
            return null;
        }

        LOG.info("GetLongPollServerResponse: \n{}\n", response);

        return new GetLongPollServerResponse(
                response.getString("key"),
                response.getString("server"),
                response.getInt("ts")
        );
    }

    /**
     * Listening to events from VK longpoll server
     * and call callbacks on events.
     * You can override only necessary methods in callback to get necessary events.
     */
    private void startListening() {

        LOG.info("Started listening to events from VK LongPoll server...");

        while (longpollIsOn) {

            JSONObject response;
            String responseString = "{}";

            try {
                Map<String, Object> values = new HashMap<>();
                values.put("act", "a_check");
                values.put("key", key);
                values.put("ts", ts);
                values.put("wait", wait);

                String url = server + "?act=a_check&key=" + key + "&ts=" + ts + "&wait=" + wait + "&mode=" + mode + "&version=" + version + "&msgs_limit=100000";

                responseString = Connection.getRequestResponse(url);

                response = new JSONObject(responseString);

            } catch (JSONException ignored) {
                LOG.error("Some error occurred, no updates got from longpoll server: {}", responseString);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored1) {
                }
                continue;
            }

            if (logUpdates) {
                LOG.info("Response of getting updates: \n{}\n", response);
            }

            if (response.has("failed")) {

                int code = response.getInt("failed");

                LOG.error("Response of VK LongPoll fallen with error code {}", code);

                if (code == 4) {
                    version = response.getInt("max_version");
                } else {
                    if (response.has("ts")) {
                        ts = response.getInt("ts");
                    }

                }
                setData(null, null, null, null, null);
            } else {

                if (response.has("ts"))
                    ts = response.getInt("ts");

                if (response.has("pts"))
                    this.pts = response.getInt("pts");

                if (this.updatesHandler.callbacksCount() > 0 || this.updatesHandler.commandsCount() > 0 || this.updatesHandler.chatCallbacksCount() > 0) {

                    if (response.has("ts") && response.has("updates")) {

                        this.updatesHandler.handle(response.getJSONArray("updates"));

                    } else {
                        LOG.error("Bad response from VK LongPoll server: no `ts` or `updates` array: {}", response);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }
        }
    }

    /**
     * If the client need to start typing
     * after receiving message
     * and until client's message is sent
     * @param enable true or false
     */
    public void enableTyping(boolean enable) {
        this.updatesHandler.sendTyping = enable;
    }

    public void enableLoggingUpdates(boolean enable) {
        this.logUpdates = enable;
    }
}