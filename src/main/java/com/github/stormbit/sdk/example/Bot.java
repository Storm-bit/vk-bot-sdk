package com.github.stormbit.sdk.example;

import com.github.stormbit.sdk.clients.User;
import com.github.stormbit.sdk.objects.Message;
import com.github.stormbit.sdk.utils.vkapi.Auth;
import net.dongliu.commons.collection.Pair;
import org.apache.log4j.PropertyConfigurator;

import java.util.Scanner;

@SuppressWarnings("unchecked")
public class Bot {
    private static final String login = "+79313648702";
    private static final String password = "709asdfgh123$$$$$$123";

    public static void main(String[] args) {
        // Logging
        String log4jConfPath = "log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);

        User user = new User(login, password, new Auth.Listener() {
            @Override
            public Pair<String, Boolean> two_factor() {
                Scanner scanner = new Scanner(System.in);
                System.out.print("Введите код: ");
                return Pair.of(scanner.nextLine(), true);
            }
        });

        new Message()
                .from(user)
                .to(337243982)
                .text("Hello, World!")
                .send();

        user.onMessage(message -> System.out.println(message.getText()));
    }
}
