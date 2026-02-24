package ksh.tryptobackend.acceptance.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;

public class HealthCheckStepDefinition {

    private final CommonApiClient commonApiClient;

    public HealthCheckStepDefinition(CommonApiClient commonApiClient) {
        this.commonApiClient = commonApiClient;
    }

    @Given("헬스체크 API를 호출하면")
    public void 헬스체크_API를_호출하면() {
        commonApiClient.get("/actuator/health");
    }

    @Then("응답 상태코드는 {int}이다")
    public void 응답_상태코드는_이다(int statusCode) {
        commonApiClient.getLastResponse()
            .expectStatus().isEqualTo(statusCode);
    }

    @Then("응답의 status는 {string}이다")
    public void 응답의_status는_이다(String status) {
        commonApiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.status").isEqualTo(status);
    }
}
