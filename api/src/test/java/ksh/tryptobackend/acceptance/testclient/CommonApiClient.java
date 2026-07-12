package ksh.tryptobackend.acceptance.testclient;

import io.cucumber.spring.ScenarioScope;
import ksh.tryptobackend.user.application.port.out.SessionCommandPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.client.RestTestClient;

@Component
@ScenarioScope
public class CommonApiClient {

    private static final String SESSION_COOKIE_NAME = "SESSION";

    private final RestTestClient restTestClient;
    private final SessionCommandPort sessionCommandPort;

    private RestTestClient.ResponseSpec lastResponse;
    private String sessionId;

    public CommonApiClient(RestTestClient restTestClient, SessionCommandPort sessionCommandPort) {
        this.restTestClient = restTestClient;
        this.sessionCommandPort = sessionCommandPort;
    }

    /** 주어진 유저로 세션을 발급받아 이후 요청에 SESSION 쿠키를 실어 보낸다. */
    public void loginAs(Long userId) {
        this.sessionId = sessionCommandPort.create(userId);
    }

    /** 마지막 응답의 Set-Cookie 에서 SESSION 값을 뽑아 이후 요청에 실어 보낸다. */
    public void adoptSessionFromLastResponse() {
        lastResponse.expectHeader().value(HttpHeaders.SET_COOKIE, this::extractSession);
    }

    private void extractSession(String setCookie) {
        String prefix = SESSION_COOKIE_NAME + "=";
        if (setCookie.startsWith(prefix)) {
            String rest = setCookie.substring(prefix.length());
            int semicolon = rest.indexOf(';');
            this.sessionId = semicolon >= 0 ? rest.substring(0, semicolon) : rest;
        }
    }

    public RestTestClient.ResponseSpec get(String path) {
        RestTestClient.RequestHeadersSpec<?> spec = restTestClient.get().uri(path);
        lastResponse = attachSession(spec).exchange();
        return lastResponse;
    }

    public <T> RestTestClient.ResponseSpec post(String path, T body) {
        RestTestClient.RequestBodySpec spec = restTestClient.post().uri(path).contentType(MediaType.APPLICATION_JSON);
        attachSession(spec);
        lastResponse = spec.body(body).exchange();
        return lastResponse;
    }

    public RestTestClient.ResponseSpec post(String path) {
        RestTestClient.RequestHeadersSpec<?> spec = restTestClient.post().uri(path);
        lastResponse = attachSession(spec).exchange();
        return lastResponse;
    }

    public <T> RestTestClient.ResponseSpec put(String path, T body) {
        RestTestClient.RequestBodySpec spec = restTestClient.put().uri(path).contentType(MediaType.APPLICATION_JSON);
        attachSession(spec);
        lastResponse = spec.body(body).exchange();
        return lastResponse;
    }

    public RestTestClient.ResponseSpec getLastResponse() {
        return lastResponse;
    }

    private RestTestClient.RequestHeadersSpec<?> attachSession(RestTestClient.RequestHeadersSpec<?> spec) {
        if (sessionId != null) {
            spec.cookie(SESSION_COOKIE_NAME, sessionId);
        }
        return spec;
    }
}
