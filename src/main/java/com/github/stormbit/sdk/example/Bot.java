package com.github.stormbit.sdk.example;

import com.github.stormbit.sdk.clients.User;
import org.apache.log4j.PropertyConfigurator;

public class Bot {
    private static String login = "login";
    private static String password = "password";
    private static int myId = 581035899;

    public static void main(String[] args) {
        // Logging
        String log4jConfPath = "log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);

        User user = new User(login, password, myId);
        System.out.println(user.api().callSync("messages.send", user, "{user_id:"+myId+", message: 123213, random_id:0}"));
        // handle message arrived
        user.onMessage(message -> {
            System.out.println(message.getText());
        });

    }
}
