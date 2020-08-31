package com.github.stormbit.sdk.utils.vkapi;

import com.github.stormbit.sdk.exceptions.NotValidAuthorization;
import com.github.stormbit.sdk.exceptions.TwoFactorError;
import com.github.stormbit.sdk.utils.Utils;
import net.dongliu.commons.collection.Pair;
import net.dongliu.requests.Header;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
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
    private static final String STRING_USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36";
    private static final Header USER_AGENT = new Header("User-Agent", STRING_USER_AGENT);
    private static final String AUTH_HASH = "\\{.*?act: 'a_authcheck_code'.+?hash: '([a-z_0-9]+)'.*?}";
    public Session session;
    private Listener _listener;


    public Auth(String login, String password) {
        session = Requests.session();
        _login = login;
        _password = password;
    }

    public Auth(String login, String password, Listener listener) {
        session = Requests.session();
        _login = login;
        _password = password;
        _listener = listener;
    }

    public Auth() {
        session = Requests.session();
    }

    public Auth auth() {
        Auth a = null;
        try {

            String data = session.get("https://vk.com/")
                    .headers(USER_AGENT)
                    .send().readToText();

            FormElement form = (FormElement) Jsoup.parse(data).getElementById("quick_login_form");

            setData(form, _login, _password);

            List<Connection.KeyVal> s = form.formData();
            Collector<Connection.KeyVal, ?, Map<String, String>> eloquentCollector =
                    Collector.of(HashMap::new, (map, e) -> putUnique(map, e.key(), e.value()),
                            (m1, m2) -> {
                                m2.forEach((k, v) -> putUnique(m1, k, v));
                                return m1;
                            });

            Map<String, ?> params = s.stream().collect(eloquentCollector);

            String response = session.post(form.attr("action"))
                    .headers(USER_AGENT, new Header("Content-Length", "0"))
                    .body(params)
                    .send().readToText();

            if (response.contains("act=authcheck")) {
                response = session.get("https://vk.com/login?act=authcheck").send().readToText();
                _pass_twofactor(response);
            } else if (!response.contains("onLoginDone")) {
                throw new NotValidAuthorization("Incorrect login or password");
            } else {
                a = this;
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return a;
    }

    private String _pass_twofactor(String auth_response) throws Exception {
        Pair<String, Boolean> pair = _listener.two_factor();

        String auth_hash = Utils.regex_search(AUTH_HASH, auth_response, 1);

        Map<String, Object> values = new HashMap<>();

        values.put("act", "a_authcheck_code");
        values.put("al", "1");
        values.put("code", pair.getKey());
        values.put("remember", pair.getValue() ? 1 : 0);
        values.put("hash", auth_hash);

        String response = session.post("https://vk.com/al_login.php")
                .headers(USER_AGENT)
                .body(values)
                .send().readToText();

        JSONObject data = new JSONObject(response.replaceAll("[<!>-]", ""));
        int status = data.getJSONArray("payload").getInt(0);

        if (status == 4) { // OK
            String path = data.getJSONArray("payload").getJSONArray(1).getString(0).replaceAll("[\\\\\"]", "");
            return session.get("https://vk.com" + path).send().readToText();

        } else if (Arrays.asList(0, 4).contains(status)) { // Incorrect code
            return _pass_twofactor(auth_response);

        } else if (status == 2) {
            throw new TwoFactorError("ReCaptcha required");
        }

        throw new Exception("Two factor authentication failed");
    }

    private void setData(FormElement form, String login, String password) {
        try {
            Element loginField = form.select("[name=email]").first();
            loginField.val(login);

            Element passwordField = form.select("[name=pass]").first();
            passwordField.val(password);
            form.submit()
                    .userAgent(STRING_USER_AGENT)
                    .execute();

        } catch (UnknownHostException e) {
            System.out.println("Error, try again later");
            System.exit(-1);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <K, V> void putUnique(Map<K, V> map, K key, V v1) {
        V v2 = map.putIfAbsent(key, v1);
        if (v2 != null) throw new IllegalStateException(
                String.format("Duplicate key '%s' (attempted merging incoming value '%s' with existing '%s')", key, v1, v2)
        );
    }

    public abstract static class Listener {
        public abstract Pair<String, Boolean> two_factor();
    }

}
