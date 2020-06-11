package com.github.stormbit.sdk.utils.vkapi;

import com.github.stormbit.sdk.exceptions.NotValidAuthorization;
import net.dongliu.requests.Header;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;

/**
 * Created by Storm-bit on 03/04/2020 19:40
 */

public class Auth {
    private String _login;
    private String _password;
    public static String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36";
    public Session session;


    public Auth(String login, String password) {
        session = Requests.session();
        _login = login;
        _password = password;
    }

    public Auth() {
        session = Requests.session();
    }

    public Auth auth() {
        Auth a = null;
        try {

            String data = session.get("https://vk.com/")
                    .headers(new Header("User-Agent", USER_AGENT))
                    .send().readToText();

            FormElement form = (FormElement) Jsoup.parse(data).getElementById("quick_login_form");

            setData(form, _login, _password);

            List<Connection.KeyVal> s = form.formData();
            Collector<Connection.KeyVal, ?, Map<String, String>> eloquentCollector =
                    Collector.of(HashMap::new, (map, e) -> putUnique(map, e.key(), e.value()),
                            (m1, m2) -> { m2.forEach((k, v) -> putUnique(m1, k, v)); return m1; });

            Map<String, ?> params = s.stream().collect(eloquentCollector);

            String response = session.post(form.attr("action"))
                    .headers(new Header("User-Agent", USER_AGENT), new Header("Content-Length", "0"))
                    .body(params)
                    .send().readToText();

            if (!response.contains("onLoginDone")) {
                throw new NotValidAuthorization("Incorrect login or password");
            } else {
                a = this;
            }

        } catch (NotValidAuthorization e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return a;
    }

    private void setData(FormElement form, String login, String password) {
        try {
            Element loginField = form.select("[name=email]").first();
            loginField.val(login);

            Element passwordField = form.select("[name=pass]").first();
            passwordField.val(password);
            form.submit()
                    .userAgent(USER_AGENT)
                    .execute();

        } catch (UnknownHostException e) {
            System.out.println("Error, try again later");
            System.exit(-1);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <K,V> void putUnique(Map<K,V> map, K key, V v1){
        V v2 = map.putIfAbsent(key, v1);
        if (v2 != null) throw new IllegalStateException(
                String.format("Duplicate key '%s' (attempted merging incoming value '%s' with existing '%s')", key, v1, v2)
        );
    }

}
