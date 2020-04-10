package com.github.stormbit.sdk.utils.vkapi;

import com.github.stormbit.sdk.callbacks.Callback;
import com.github.stormbit.sdk.clients.Client;
import com.github.stormbit.sdk.clients.User;
import com.github.stormbit.sdk.utils.Utils;
import com.github.stormbit.sdk.utils.vkapi.calls.CallAsync;
import com.github.stormbit.sdk.utils.vkapi.calls.CallSync;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple interacting with VK API
 */
public class API {

    private static final Logger LOG = LoggerFactory.getLogger(API.class);

    private static Executor executor;

    private static boolean executionStarted = false;

    /**
     * Get the token from client
     * todo Not all methods available with group tokens, and few methods available without token
     * todo Need to make client with both tokens, or any another conclusion
     *
     * @param client Client with token
     */
    public API(Client client) {
        if (!executionStarted) {
            executor = new Executor(client.get_auth());
            executionStarted = true;
        }
    }

    /**
     * todo Not all methods available with group tokens, and few methods available without token
     * todo Need to make client with both tokens, or any another conclusion
     * @param auth Auth object
     */
    public API(Auth auth) {
        if (!executionStarted) {
            executor = new Executor(auth);
            executionStarted = true;
        }
    }

    /**
     * Call to VK API
     *
     * @param method   Method name
     * @param params   Params as string, JSONObject or Map
     * @param callback Callback to return the response
     */
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
            LOG.error("Some error occured when calling VK API method {} with params {}, error is {}", method, params.toString(), e);
        }
    }

    /**
     * Call to VK API
     *
     * @param callback Callback to return the response
     * @param method   Method name
     * @param params   Floating count of params
     */
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
        } catch (Exception e) {
            LOG.error("Some error occurred when calling VK API: {0}", e);
        }
    }

    /**
     * Call to 'execute' method, because can not call API.execute inside execute.
     * More: <a href="https://vk.com/dev/execute">link</a>;
     * @param code code
     * @param user User object
     * @return JSONObject response of VK answer
     */
    public JSONObject execute(String code, User user) {

        return new JSONObject(callSync("execute", user, new JSONObject().put("code", code)));
    }

    /**
     * Execute float count of calls, up to 25
     *
     * @param calls single call to VK API or calls separated by comma.
     * @see CallAsync
     */
    public void execute(CallAsync... calls) {
        if (calls.length < 26) {
            for (CallAsync call : calls) {
                executor.execute(call);
            }
        } else {
            CallAsync[] newCalls = new CallAsync[25];
            System.arraycopy(calls, 0, newCalls, 0, 25);
            for (CallAsync call : newCalls) {
                executor.execute(call);
            }
        }
    }

    /**
     * Execute float count of calls, up to 25
     *
     * @param calls single call to VK API or calls separated by comma.
     * @param user User object
     * @return JSONArray with responses of calls
     * @see CallSync
     */
    public JSONArray execute(User user, CallSync... calls) {

        StringBuilder code = new StringBuilder("return [");

        for (int i = 0; i < calls.length; i++) {
            String codeTmp = executor.codeForExecute(calls[i]);
            code.append(codeTmp);
            if (i < calls.length - 1) {
                code.append(',');
            }
        }
        code.append("];");

        JSONObject response = null;
        try {
            response = new JSONObject(callSync("execute", user, new JSONObject().put("code", URLEncoder.encode(code.toString(), "UTF-8"))));
        } catch (UnsupportedEncodingException ignored) {
        }

        return response.getJSONArray("response");
    }

    /**
     * Call to VK API
     *
     * @param method Method name
     * @param params Params as string, JSONObject or Map
     * @param user User object
     * @return JSONObject response of VK answer
     */
    public JSONObject callSync(String method, Object params, Client user) {

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
                        Utils.get_hash(user.get_auth(), method);
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

                    var responseString = user.get_auth().session.post(Utils.URL)
                            .body(prms)
                            .send().readToText().replaceAll("[<!>-]", "");

                    return new JSONObject(new JSONObject(responseString).getJSONArray("payload").getJSONArray(1).getString(0));
                }

            }
        } catch (Exception e) {
            LOG.error("Some error occurred when calling VK API: {0}", e);
        }
        return new JSONObject();
    }

    /**
     * Call to VK API
     *
     * @param method Method name
     * @param user User object
     * @param params Floating count of params
     * @return JSONObject response of VK answer
     */

    public JSONObject callSync(String method, Client user, Object... params) {

        try {
            if (params != null) {
                if (params.length == 1) {
                    return this.callSync(method, params[0], user);
                }

                if (params.length > 1 && params.length % 2 == 0) {
                    Map<String, Object> map = new HashMap<>();

                    for(int i = 0; i < params.length - 1; i += 2) {
                        map.put(params[i].toString(), params[i + 1]);
                    }

                    return this.callSync(method, map, user);
                }
            }
        } catch (Exception e) {
            LOG.error("Some error occured when calling VK API: {0}", e);
        }

        return new JSONObject();
    }
}
