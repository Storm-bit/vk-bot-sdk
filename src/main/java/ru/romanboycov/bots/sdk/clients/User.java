package ru.romanboycov.bots.sdk.clients;

/**
 * User client, that contains important methods to work with users
 *
 * Not need now to put methods there: use API.call
 */
public class User extends Client {

    public User(String login, String password, int id) {
        super(login, password, id);
    }
}