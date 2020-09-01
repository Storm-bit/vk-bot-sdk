package com.github.stormbit.sdk.utils.vkapi.keyboard;

import com.github.stormbit.sdk.utils.vkapi.keyboard.Keyboard.Button;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class KeyboardBuilder implements Serializable {
    private Boolean one_time = false;
    private final ArrayList<List<Button>> rows = new ArrayList<>();

    public KeyboardBuilder isOneTime(boolean isOneTime) {
        this.one_time = isOneTime;

        return this;
    }

    public KeyboardBuilder buttonsRow(RowBuilder builder) {
        rows.add(builder.getButtons());

        return this;
    }

    public KeyboardBuilder locationButton(JSONObject payload) {
        rows.add(Collections.singletonList(new Button(new Button.Action(Button.Action.Type.LOCATION, payload))));

        return this;
    }

    public KeyboardBuilder vkPayButton(String hash, JSONObject payload) {
        rows.add(Collections.singletonList(new Button(new Button.Action(Button.Action.Type.VK_PAY, payload, hash))));

        return this;
    }

    public KeyboardBuilder vkAppButton(String label, JSONObject payload) {
        rows.add(Collections.singletonList(new Button(new Button.Action(Button.Action.Type.LOCATION, label, payload))));

        return this;
    }

    public KeyboardBuilder openLinkButton(String label, String link, JSONObject payload) {
        rows.add(Collections.singletonList(new Button(new Button.Action(Button.Action.Type.LOCATION, label, link, payload))));

        return this;
    }

    public Keyboard build() {
        return new Keyboard(one_time, rows);
    }
}


