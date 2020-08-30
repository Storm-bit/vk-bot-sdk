package com.github.stormbit.sdk.utils.vkapi.executors;

import com.github.stormbit.sdk.utils.Utils;
import com.github.stormbit.sdk.utils.vkapi.Auth;
import com.github.stormbit.sdk.utils.vkapi.Executor;
import com.github.stormbit.sdk.utils.vkapi.calls.CallAsync;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Created by Storm-bit
 * Executor for users
 */
public class ExecutorUser extends Executor {

    public ExecutorUser(Auth auth) {
        super(auth);
    }

    @Override
    protected void executing() {

        List<CallAsync> tmpQueue = new ArrayList<>();
        int count = 0;

        for (Iterator<CallAsync> iterator = queue.iterator(); iterator.hasNext() && count < 25; count++) {
            tmpQueue.add(iterator.next());
        }

        if (!tmpQueue.isEmpty()) {
            for (CallAsync item : tmpQueue) {
                String method = item.getMethodName();
                JSONObject params = item.getParams();

                if (!Utils._hashes.has(method)) {
                    Utils.get_hash(_auth, method);
                }

                queue.removeAll(tmpQueue);

                JSONObject data = new JSONObject();
                data.put("act", "a_run_method");
                data.put("al", 1);
                data.put("hash", Utils._hashes.get(method));
                data.put("method", method);
                data.put("param_v", Utils.version);

                for (String key : params.keySet()) {
                    data.put("param_" + key, params.get(key));
                }

                Map<String, Object> prms = new HashMap<>();
                for (String key : data.keySet()) {
                    prms.put(key, data.get(key));
                }

                // Execute
                if (count > 0) {

                    String responseString = _auth.session.post(Utils.URL)
                            .body(prms)
                            .send().readToText().replaceAll("[<!>]", "").substring(2);

                    if (LOG_REQUESTS) {
                        LOG.error("New executing request response: {}", responseString);
                    }

                    JSONObject response;

                    try {
                        response = new JSONObject(new JSONObject(responseString).getJSONArray("payload").getJSONArray(1).getString(0));
                    } catch (JSONException e) {
                        tmpQueue.forEach(call -> call.getCallback().onResult("false"));
                        LOG.error("Bad response from executing: {}, params: {}", responseString, data.toString());
                        return;
                    }

                    if (!response.has("response")) {
                        LOG.error("No 'response' object when executing code, VK response: {}", response);
                        tmpQueue.forEach(call -> call.getCallback().onResult("false"));
                        return;
                    }

                    Object responses = response.get("response");

                    IntStream.range(0, count).forEachOrdered(i -> tmpQueue.get(i).getCallback().onResult(responses));
                }
            }
        }
    }
}
