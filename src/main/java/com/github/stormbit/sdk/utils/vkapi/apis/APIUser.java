package com.github.stormbit.sdk.utils.vkapi.apis;

import com.github.stormbit.sdk.callbacks.Callback;
import com.github.stormbit.sdk.clients.Client;
import com.github.stormbit.sdk.utils.Utils;
import com.github.stormbit.sdk.utils.vkapi.API;
import com.github.stormbit.sdk.utils.vkapi.calls.CallAsync;
import com.github.stormbit.sdk.utils.vkapi.executors.ExecutorUser;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Storm-bit
 * API for users
 */
public class APIUser extends API {

    public APIUser(Client client) {
        super(client, new ExecutorUser(client.auth()));
    }

    @Override
    public void call(String method, Object params, Callback<Object> callback) {

        try {
            JSONObject parameters = new JSONObject();

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
        } catch (Exception e) {
            LOG.error("Some error occurred when calling VK API method {} with params {}, error is {}", method, params.toString(), e.getMessage());
        }
    }

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

    @Override
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

                    if (!Utils._hashes.has(method)) {
                        Utils.get_hash(client.auth(), method);
                    }

                    JSONObject data = new JSONObject();
                    data.put("act", "a_run_method");
                    data.put("al", 1);
                    data.put("hash", Utils._hashes.get(method));
                    data.put("method", method);
                    data.put("param_v", Utils.version);

                    for (String key : parameters.keySet()) {
                        data.put("param_" + key, parameters.get(key));
                    }

                    Map<String, Object> prms = new HashMap<>();
                    for (String key : data.keySet()) {
                        prms.put(key, data.get(key));
                    }

                    String responseString = client.auth().session.post(Utils.URL)
                            .body(prms)
                            .send().readToText().replaceAll("[<!>]", "").substring(2);

                    return new JSONObject(new JSONObject(responseString).getJSONArray("payload").getJSONArray(1).getString(0));
                }

            }
        } catch (Exception e) {
            LOG.error("Some error occurred when calling VK API: {}", e.getMessage());
        }
        return new JSONObject();
    }

    @Override
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
