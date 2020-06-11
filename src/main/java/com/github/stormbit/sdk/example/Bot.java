package com.github.stormbit.sdk.example;

import com.github.stormbit.sdk.clients.User;
import com.github.stormbit.sdk.objects.Message;
import org.apache.log4j.PropertyConfigurator;

@SuppressWarnings("unchecked")
public class Bot {
    private static final String login = "login";
    private static final String password = "password";
    private static final int id = 10000000;

    public static void main(String[] args) {
        // Logging
        String log4jConfPath = "log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);

        User user = new User(login, password, id);

        new Message()
                .from(user)
                .to(337243982)
                .text("Hello, World!")
                .send();

        user.onMessage(message -> System.out.println(message.getText()));
    }
}
