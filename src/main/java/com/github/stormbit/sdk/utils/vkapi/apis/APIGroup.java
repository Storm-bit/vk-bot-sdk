package com.github.stormbit.sdk.utils.vkapi.apis;

import com.github.stormbit.sdk.callbacks.Callback;
import com.github.stormbit.sdk.clients.Client;
import com.github.stormbit.sdk.utils.Utils;
import com.github.stormbit.sdk.utils.vkapi.API;
import com.github.stormbit.sdk.utils.vkapi.calls.CallAsync;
import com.github.stormbit.sdk.utils.vkapi.executors.ExecutorGroup;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Storm-bit on 03/04/2020 19:40
 * API for tokens
 */
public class APIGroup extends API {
    /**
     * Get the token from client
     *
     * @param client Client
     */
    public APIGroup(Client client) {
        super(client, new ExecutorGroup(client, client.auth()));
    }

    /**
     * Call to VK API
     *
     * @param method   Method name
     * @param params   Params as string, JSONObject or Map
     * @param callback Callback to return the response
     */
    @Override
    public void call(String method, Object params, Callback<Object> callback) {

        try {
            JSONObject parameters = new JSONObject();

            if (params != null) {
                boolean good = false;

                // Work with map
                if (params instanceof Map) {

                    parameters = new JSONObject((Map) params);
                    good = true;
                }

                // with JO
                if (params instanceof JSONObject) {
                    parameters = (JSONObject) params;
                    good = true;
                }

                // or string
                if (params instanceof String) {
                    String s = params.toString();
                    if (s.startsWith("{")) {
                        parameters = new JSONObject(s);
                        good = true;
                    } else {
                        if (s.contains("&") && s.contains("=")) {
                            parameters = Utils.explodeQuery(s);
                            good = true;
                        }
                    }
                }

                if (good) {
                    CallAsync call = new CallAsync(method, parameters, callback);
                    executor.execute(call);
                }
            }
        } catch (Exception e) {
            LOG.error("Some error occurred when calling VK API method {} with params {}, error is {}", method, params.toString(), e.getMessage());
        }
    }

    /**
     * Call to VK API
     *
     * @param callback Callback to return the response
     * @param method   Method name
     * @param params   Floating count of params
     */
    @Override
    public void call(Callback<Object> callback, String method, Object... params) {

        try {
            if (params != null) {

                if (params.length == 1) {
                    this.call(method, params[0], callback);
                }

                if (params.length > 1) {

                    if (params.length % 2 == 0) {
                        Map<String, Object> map = new HashMap<>();

                        for (int i = 0; i < params.length - 1; i += 2) {
                            map.put(params[i].toString(), params[i + 1]);
                        }

                        this.call(method, map, callback);
                    }
                }
            }
            this.call(method, new HashMap<String, Object>(), callback);
        } catch (Exception e) {
            LOG.error(String.format("Some error occurred when calling VK API: {%s}", e));
        }
    }

    /**
     * Call to VK API
     *
     * @param method Method name
     * @param params Params as string, JSONObject or Map
     * @return JSONObject response of VK answer
     */
    public JSONObject callSync(String method, Object params) {

        try {
            JSONObject parameters = new JSONObject();

            if (params != null) {
                boolean good = false;

                // Work with map
                if (params instanceof Map) {

                    parameters = new JSONObject((Map) params);
                    good = true;
                }

                // with JO
                if (params instanceof JSONObject) {
                    parameters = (JSONObject) params;
                    good = true;
                }

                // or string
                if (params instanceof String) {
                    String s = params.toString();
                    if (s.startsWith("{")) {
                        parameters = new JSONObject(s);
                        good = true;
                    } else {
                        if (s.contains("&") && s.contains("=")) {
                            parameters = Utils.explodeQuery(s);
                            good = true;
                        }
                    }
                }

                if (good) {

                    JSONObject data = new JSONObject();
                    data.put("v", Utils.version);
                    data.put("access_token", client.getToken());

                    for (String key : parameters.keySet()) {
                        data.put(key, parameters.get(key));
                    }

                    Map<String, Object> prms = new HashMap<>();
                    for (String key : data.keySet()) {
                        prms.put(key, data.get(key));
                    }

                    String responseString = client.auth().session.post("https://api.vk.com/method/" + method)
                            .body(prms)
                            .send().readToText().replaceAll("[<!>]", "");

                    return new JSONObject(responseString);
                }

            }
        } catch (Exception e) {
            LOG.error("Some error occurred when calling VK API: {}", e.getMessage());
        }
        return new JSONObject();
    }

    /**
     * Call to VK API
     *
     * @param method Method name
     * @param params Floating count of params
     * @return JSONObject response of VK answer
     */

    public JSONObject callSync(String method, Object... params) {

        try {
            if (params != null) {
                if (params.length == 1) {
                    return this.callSync(method, params[0]);
                }

                if (params.length > 1 && params.length % 2 == 0) {
                    Map<String, Object> map = new HashMap<>();

                    for (int i = 0; i < params.length - 1; i += 2) {
                        map.put(params[i].toString(), params[i + 1]);
                    }

                    return this.callSync(method, map);
                }
            }
            return this.callSync(method, new HashMap<String, Object>());
        } catch (Exception e) {
            LOG.error("Some error occurred when calling VK API: {}", e.getMessage());
        }

        return new JSONObject();
    }
}
