package ksh.tryptobackend.user.adapter.out.oauth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

class FakeOAuthServer {

    private static final String TOKEN_PATH = "/token";
    private static final String USER_INFO_PATH = "/userinfo";
    private static final String TOKEN_RESPONSE = "{\"access_token\":\"fake-access-token\"}";
    private static final int OK = 200;

    private final HttpServer server;
    private final String userInfoResponse;
    private final Map<String, String> tokenRequestForm = new HashMap<>();

    private FakeOAuthServer(HttpServer server, String userInfoResponse) {
        this.server = server;
        this.userInfoResponse = userInfoResponse;
    }

    static FakeOAuthServer start(String userInfoResponse) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        FakeOAuthServer fake = new FakeOAuthServer(server, userInfoResponse);
        server.createContext(TOKEN_PATH, fake::handleToken);
        server.createContext(USER_INFO_PATH, fake::handleUserInfo);
        server.start();
        return fake;
    }

    void stop() {
        server.stop(0);
    }

    String tokenUri() {
        return baseUri() + TOKEN_PATH;
    }

    String userInfoUri() {
        return baseUri() + USER_INFO_PATH;
    }

    Map<String, String> tokenRequestForm() {
        return Map.copyOf(tokenRequestForm);
    }

    private String baseUri() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private void handleToken(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        tokenRequestForm.clear();
        tokenRequestForm.putAll(parseForm(body));
        respond(exchange, TOKEN_RESPONSE);
    }

    private void handleUserInfo(HttpExchange exchange) throws IOException {
        respond(exchange, userInfoResponse);
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> form = new HashMap<>();
        if (body.isBlank()) {
            return form;
        }
        for (String pair : body.split("&")) {
            int separator = pair.indexOf('=');
            String name = URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8);
            form.put(name, value);
        }
        return form;
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(OK, payload.length);
        exchange.getResponseBody().write(payload);
        exchange.close();
    }
}
