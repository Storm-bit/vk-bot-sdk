package com.github.stormbit.sdk.utils.vkapi;

import com.github.stormbit.sdk.clients.Client;
import com.github.stormbit.sdk.utils.vkapi.calls.Call;
import com.github.stormbit.sdk.utils.vkapi.calls.CallAsync;
import com.github.stormbit.sdk.utils.vkapi.calls.CallSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by PeterSamokhin on 28/09/2017 21:59
 * Updated by RomanBoycov on 03/04/2020 19:40
 * <p>
 * Best way to use VK API: you can call up to 25 vk api methods by call execute once
 * Because without execute you only can call up to 3 methods per second
 * <p>
 * See more: <a href="https://vk.com/dev/execute">link</a>
 */
public abstract class Executor {

    protected static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    public static boolean LOG_REQUESTS = false;

    /**
     * We can call 'execute' method no more than three times per second.
     * 1000/3 ~ 333 milliseconds
     */
    private static final int delay = 335;

    /**
     * Queue of requests
     */
    protected volatile List<CallAsync> queue = new ArrayList<>();

    protected final Auth _auth;


    public Executor(Auth auth) {
        _auth = auth;

        Client.scheduler.scheduleWithFixedDelay(this::executing, 0, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Method that makes 'execute' requests
     * with first 25 calls from queue.
     */
    protected abstract void executing();

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
