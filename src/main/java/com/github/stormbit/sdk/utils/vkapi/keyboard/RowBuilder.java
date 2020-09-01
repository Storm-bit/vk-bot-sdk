package com.github.stormbit.sdk.utils.vkapi.keyboard;

import com.github.stormbit.sdk.utils.vkapi.keyboard.Keyboard.Button;
import org.json.JSONObject;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class RowBuilder {
    private ArrayList<Button> buttons = new ArrayList<>();

    public RowBuilder primaryButton(String label, JSONObject payload) {
        addButton(label, payload, Button.ButtonColor.PRIMARY);

        return this;
    }

    public RowBuilder defaultButton(String label, JSONObject payload) {
        addButton(label, payload, Button.ButtonColor.DEFAULT);

        return this;
    }

    public RowBuilder positiveButton(String label, JSONObject payload) {
        addButton(label, payload, Button.ButtonColor.POSITIVE);

        return this;
    }

    public RowBuilder negativeButton(String label, JSONObject payload) {
        addButton(label, payload, Button.ButtonColor.NEGATIVE);

        return this;
    }



    public RowBuilder primaryButton(String label) {
        addButton(label, new JSONObject(), Button.ButtonColor.PRIMARY);

        return this;
    }

    public RowBuilder defaultButton(String label) {
        addButton(label, new JSONObject(), Button.ButtonColor.DEFAULT);

        return this;
    }

    public RowBuilder positiveButton(String label) {
        addButton(label, new JSONObject(), Button.ButtonColor.POSITIVE);

        return this;
    }

    public RowBuilder negativeButton(String label) {
        addButton(label, new JSONObject(), Button.ButtonColor.NEGATIVE);

        return this;
    }

    private void addButton(String label, JSONObject payload, Button.ButtonColor color) {
        buttons.add(new Keyboard.Button(color, new Keyboard.Button.Action(Button.Action.Type.TEXT, label, payload)));
    }

    public ArrayList<Button> getButtons() {
        return buttons;
    }

    public void setButtons(ArrayList<Button> buttons) {
        this.buttons = buttons;
    }
}
