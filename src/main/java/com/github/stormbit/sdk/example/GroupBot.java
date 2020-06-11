package com.github.stormbit.sdk.example;

import com.github.stormbit.sdk.clients.Group;
import com.github.stormbit.sdk.objects.Message;
import org.apache.log4j.PropertyConfigurator;

// Example echo bot
@SuppressWarnings("unchecked")
public class GroupBot {
    private static final String token = "390ed49357d6cba12d431ccc1dca10b9a6fc1f7673584a694d43a957bcb4dab7db305353e5f795c37b50e";
    private static final Integer id = 186953463;

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
