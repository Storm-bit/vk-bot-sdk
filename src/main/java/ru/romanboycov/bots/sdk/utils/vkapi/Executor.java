package ru.romanboycov.bots.sdk.utils.vkapi;

import ru.romanboycov.bots.sdk.utils.Utils;
import ru.romanboycov.bots.sdk.utils.vkapi.calls.Call;
import ru.romanboycov.bots.sdk.utils.vkapi.calls.CallAsync;
import ru.romanboycov.bots.sdk.utils.vkapi.calls.CallSync;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.romanboycov.bots.sdk.clients.Client;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Best way to use VK API: you can call up to 25 vk api methods by call execute once
 * Because without execute you only can call up to 3 methods per second
 * <p>
 * See more: <a href="https://vk.com/dev/execute">link</a>
 */
public class Executor {

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    public static boolean LOG_REQUESTS = false;

    /**
     * We can call 'execute' method no more than three times per second.
     * 1000/3 ~ 333 milliseconds
     */
    private static final int delay = 335;

    /**
     * Queue of requests
     */
    private volatile List<CallAsync> queue = new ArrayList<>();

    private final Auth _auth;


    public Executor(Auth auth) {
        _auth = auth;

        Client.scheduler.scheduleWithFixedDelay(this::executing, 0, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Method that makes 'execute' requests
     * with first 25 calls from queue.
     */
    private void executing() {

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
                            .send().readToText().replaceAll("[<!>-]", "");

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

    /**
     * Method that makes string in json format from call object.
     *
     * @param call Call object
     * @return String 'API.method.name({param:value})'
     * @see Call
     * @see CallAsync
     * @see CallSync
     */
    public String codeForExecute(Call call) {

        return "API." + call.getMethodName() + '(' + call.getParams().toString() + ')';
    }

    /**
     * Method that puts all requests in a queue.
     *
     * @param call Call to be executed.
     */
    public void execute(CallAsync call) {
        queue.add(call);
    }
}
