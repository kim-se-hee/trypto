package ksh.tryptobackend.acceptance.testclient;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.client.RestTestClient;

@Component
@ScenarioScope
public class CommonApiClient {

    private final RestTestClient restTestClient;
    private RestTestClient.ResponseSpec lastResponse;

    public CommonApiClient(RestTestClient restTestClient) {
        this.restTestClient = restTestClient;
    }

    public RestTestClient.ResponseSpec get(String path) {
        lastResponse = restTestClient.get().uri(path).exchange();
        return lastResponse;
    }

    public <T> RestTestClient.ResponseSpec post(String path, T body) {
        lastResponse = restTestClient.post().uri(path)
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(body)
            .exchange();
        return lastResponse;
    }

    public RestTestClient.ResponseSpec post(String path) {
        lastResponse = restTestClient.post().uri(path).exchange();
        return lastResponse;
    }

    public <T> RestTestClient.ResponseSpec put(String path, T body) {
        lastResponse = restTestClient.put().uri(path)
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(body)
            .exchange();
        return lastResponse;
    }

    public RestTestClient.ResponseSpec getLastResponse() {
        return lastResponse;
    }
}
