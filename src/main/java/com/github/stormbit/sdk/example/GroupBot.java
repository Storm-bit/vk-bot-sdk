package com.github.stormbit.sdk.example;

import com.github.stormbit.sdk.clients.Group;
import com.github.stormbit.sdk.objects.Message;
import org.apache.log4j.PropertyConfigurator;

/**
 * Created by Storm-bit
 *
 * Example echo bot
 */
@SuppressWarnings("unchecked")
public class GroupBot {
    private static final String token = "";
    private static final Integer id = 0;

    public static void main(String[] args) {
        // Logging
        String log4jConfPath = "log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);

        Group group = new Group(token, id);

        group.onMessage(message -> new Message()
                .from(group)
                .to(message.authorId())
                .text(message.getText())
                .send());
    }
}
