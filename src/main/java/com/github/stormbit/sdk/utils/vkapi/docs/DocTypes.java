package com.github.stormbit.sdk.utils.vkapi.docs;

/**
 * Types of docs
 */
public enum DocTypes {

    DOC("doc"),
    AUDIO_MESSAGE("audio_message"),
    GRAFFITI("graffiti");

    String type;

    DocTypes(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
