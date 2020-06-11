package com.github.stormbit.sdk.utils.vkapi;

import com.github.stormbit.sdk.callbacks.Callback;
import com.github.stormbit.sdk.clients.Client;
import com.github.stormbit.sdk.utils.vkapi.calls.CallAsync;
import com.github.stormbit.sdk.utils.vkapi.calls.CallSync;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by PeterSamokhin on 28/09/2017 21:59
 * Updated by Storm-bit on 03/04/2020 19:40
 *
 * Simple interacting with VK API
 */
public abstract class API {

    protected final Logger LOG = LoggerFactory.getLogger(API.class);

    protected Executor executor;
    protected Client client;

    protected boolean executionStarted = false;

    /**
     * Get the token from client
     * @param client Client
     * @param executor Executor
     */
    public API(Client client, Executor executor) {
        this.client = client;
        this.executor = executor;
        if (!executionStarted) {
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
    public abstract void call(String method, Object params, Callback<Object> callback);

    /**
     * Call to VK API
     *
     * @param callback Callback to return the response
     * @param method   Method name
     * @param params   Floating count of params
     */
    public abstract void call(Callback<Object> callback, String method, Object... params);

    /**
     * Call to 'execute' method, because can not call API.execute inside execute.
     * More: <a href="https://vk.com/dev/execute">link</a>;
     * @param code code
     * @return JSONObject response of VK answer
     */
    public JSONObject execute(String code) {

        return new JSONObject(callSync("execute", new JSONObject().put("code", code)));
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
     * @return JSONArray with responses of calls
     * @see CallSync
     */
    public JSONArray execute(CallSync... calls) {

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
            response = new JSONObject(callSync("execute", new JSONObject().put("code", URLEncoder.encode(code.toString(), "UTF-8"))));
        } catch (UnsupportedEncodingException ignored) {
        }

        return response.getJSONArray("response");
    }

    /**
     * Call to VK API
     *
     * @param method Method name
     * @param params Params as string, JSONObject or Map
     * @return JSONObject response of VK answer
     */
    public abstract JSONObject callSync(String method, Object params);

    /**
     * Call to VK API
     *
     * @param method Method name
     * @param params Floating count of params
     * @return JSONObject response of VK answer
     */

    public abstract JSONObject callSync(String method, Object... params);
}
