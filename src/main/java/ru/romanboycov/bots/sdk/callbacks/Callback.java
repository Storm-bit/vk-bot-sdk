package ru.romanboycov.bots.sdk.callbacks;

/**
 * For all callbacks compatibility.
 */
public interface Callback<T> {

    void onResult(T object);
}
