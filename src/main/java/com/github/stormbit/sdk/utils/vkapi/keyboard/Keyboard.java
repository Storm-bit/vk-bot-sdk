package com.github.stormbit.sdk.utils.vkapi.keyboard;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class Keyboard implements Serializable {
    private boolean one_time = false;
    private List<List<Button>> buttons = new ArrayList<>();

    public Keyboard(boolean isOneTime, List<List<Button>> buttons) {
        this.one_time = isOneTime;
        this.buttons = buttons;
    }

    public Keyboard(boolean isOneTime) {
        this.one_time = isOneTime;
    }

    public Keyboard(List<List<Button>> buttons) {
        this.buttons = buttons;
    }

    public boolean isOne_time() {
        return one_time;
    }

    public void setOne_time(boolean one_time) {
        this.one_time = one_time;
    }

    public List<List<Button>> getButtons() {
        return buttons;
    }

    public void setButtons(List<List<Button>> buttons) {
        this.buttons = buttons;
    }

    public static class Button implements Serializable {
        private Action action = new Action();
        private String color = ButtonColor.DEFAULT.color;

        public Action getAction() {
            return action;
        }

        public void setAction(Action action) {
            this.action = action;
        }

        public String getColor() {
            return color;
        }

        public void setColor(ButtonColor color) {
            this.color = color.color;
        }

        public Button(ButtonColor color, Action action) {
            this.color = color.color;
            this.action = action;
        }

        public Button(ButtonColor color) {
            this.color = color.color;
        }

        public Button(Action action) {
            this.action = action;
        }

        public Button() {
        }

        public static class Action implements Serializable {
            private String type;
            private String label;
            private String payload = "{}";
            private String hash;
            private String link;

            public String getType() {
                return type;
            }

            public void setType(Type type) {
                this.type = type.getType();
            }

            public String getLabel() {
                return label;
            }

            public void setLabel(String label) {
                this.label = label;
            }

            public String getPayload() {
                return payload;
            }

            public void setPayload(JSONObject payload) {
                this.payload = payload.toString();
            }

            public String getHash() {
                return hash;
            }

            public void setHash(String hash) {
                this.hash = hash;
            }

            public String getLink() {
                return link;
            }

            public void setLink(String link) {
                this.link = link;
            }

            public Action(Type type, String label, JSONObject payload, String hash, String link) {
                this.type = type.getType();
                this.label = label;
                this.payload = payload.toString();
                this.hash = hash;
                this.link = link;
            }

            public Action(Type type, String label, JSONObject payload) {
                this.type = type.getType();
                this.label = label;
                this.payload = payload.toString();
            }

            public Action(Type type, String label, JSONObject payload, String hash) {
                this.type = type.getType();
                this.label = label;
                this.payload = payload.toString();
                this.hash = hash;
            }

            public Action(Type type, String label, String link, JSONObject payload) {
                this.type = type.getType();
                this.label = label;
                this.payload = payload.toString();
                this.link = link;
            }

            public Action(Type type, JSONObject payload, String hash) {
                this.type = type.getType();
                this.payload = payload.toString();
                this.hash = hash;
            }

            public Action(String label, JSONObject payload) {
                this.label = label;
                this.payload = payload.toString();
            }

            public Action(Type type, JSONObject payload) {
                this.type = type.getType();
                this.payload = payload.toString();
            }

            public Action(Type type, String label) {
                this.type = type.getType();
                this.label = label;
            }

            public Action(Type type) {
                this.type = type.getType();
            }

            public Action() {
            }

            public enum Type implements Serializable {
                TEXT("text"),
                LOCATION("location"),
                VK_PAY("vkpay"),
                OPEN_APP("open_app"),
                OPEN_LINK("open_link"),
                OPEN_PHOTO("open_photo");

                private final String type;

                Type(String name) {
                    this.type = name;
                }

                public String getType() {
                    return type;
                }
            }
        }

        public enum ButtonColor implements Serializable {
            PRIMARY("primary"),
            DEFAULT("secondary"),
            NEGATIVE("negative"),
            POSITIVE("positive");

            private final String color;

            ButtonColor(String color) {
                this.color = color;
            }

            public String getColor() {
                return color;
            }
        }
    }

    @Override
    public String toString() {
        return new JSONObject(this).toString();
    }
}
