package com.github.stormbit.sdk.longpoll;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Queue of updates
 */
class Queue {

    /**
     * List of updates that we need to handle
     */
    protected volatile CopyOnWriteArrayList<JSONArray> updates = new CopyOnWriteArrayList<>();
    protected volatile CopyOnWriteArrayList<JSONObject> updates2 = new CopyOnWriteArrayList<>();

    /**
     * We add all of updates from longpoll server
     * to queue
     *
     * @param elements Array of updates
     */
    protected void putAll(JSONArray elements) {
        elements.forEach(item -> {
            if (item instanceof JSONArray) {
                updates.add((JSONArray) item);
            } else {
                updates2.add((JSONObject) item);
            }
        });
    }

    /**
     * Analog method of 'shift()' method from javascript
     *
     * @return First element of list, and then remove it
     */
    protected JSONArray shift() {
        if (this.updates.size() > 0) {
            JSONArray answer = this.updates.get(0);
            this.updates.remove(0);
            return answer;
        }
        return new JSONArray();
    }

    protected JSONObject shift2() {
        if (this.updates2.size() > 0) {
            JSONObject answer = this.updates2.get(0);
            this.updates2.remove(0);
            return answer;
        }
        return new JSONObject();
    }
}
