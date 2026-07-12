package ksh.tryptobackend.acceptance.steps.user;

import io.cucumber.java.en.When;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;

public class DeleteAccountStepDefinition {

    private final CommonApiClient apiClient;

    public DeleteAccountStepDefinition(CommonApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @When("회원 탈퇴를 요청한다")
    public void 회원_탈퇴를_요청한다() {
        apiClient.delete("/api/users/me");
    }

    @When("세션 쿠키 없이 회원 탈퇴를 요청한다")
    public void 세션_쿠키_없이_회원_탈퇴를_요청한다() {
        apiClient.delete("/api/users/me");
    }
}
