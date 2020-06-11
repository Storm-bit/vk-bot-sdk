package com.github.stormbit.sdk.clients;

/**
 * User client, that contains important methods to work with users
 *
 * Not need now to put methods there: use API.call
 */
public class User extends Client {

    /**
     * Default constructor
     * @param login Login of your VK bot
     * @param password Password of your VK bot
     */

    public User(String login, String password) {
        super(login, password);
    }
}