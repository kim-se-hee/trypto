package ksh.tryptobackend.acceptance.steps.user;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.LocalDateTime;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import ksh.tryptobackend.user.adapter.out.persistence.entity.SocialAccountJpaEntity;
import ksh.tryptobackend.user.adapter.out.persistence.entity.UserJpaEntity;
import ksh.tryptobackend.user.adapter.out.persistence.repository.SocialAccountJpaRepository;
import ksh.tryptobackend.user.adapter.out.persistence.repository.UserJpaRepository;
import ksh.tryptobackend.user.domain.model.SocialAccount;
import ksh.tryptobackend.user.domain.model.User;
import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import org.springframework.http.HttpHeaders;

public class LogoutStepDefinition {

    private static final String SESSION_COOKIE_NAME = "SESSION";

    private final CommonApiClient apiClient;
    private final UserJpaRepository userJpaRepository;
    private final SocialAccountJpaRepository socialAccountJpaRepository;

    public LogoutStepDefinition(
            CommonApiClient apiClient,
            UserJpaRepository userJpaRepository,
            SocialAccountJpaRepository socialAccountJpaRepository) {
        this.apiClient = apiClient;
        this.userJpaRepository = userJpaRepository;
        this.socialAccountJpaRepository = socialAccountJpaRepository;
    }

    @Given("로그인된 사용자가 존재한다")
    public void 로그인된_사용자가_존재한다() {
        SocialAccountJpaEntity account = socialAccountJpaRepository.save(SocialAccountJpaEntity.fromDomain(
                SocialAccount.register(SocialIdentity.of(Provider.KAKAO, "test-logout"), LocalDateTime.now())));
        UserJpaEntity saved = userJpaRepository.save(
                UserJpaEntity.fromDomain(User.create(account.getId(), "로그아웃유저", false, LocalDateTime.now())));
        apiClient.loginAs(saved.getId());
    }

    @When("로그아웃을 요청한다")
    public void 로그아웃을_요청한다() {
        apiClient.post("/api/auth/logout");
    }

    @When("세션 쿠키 없이 로그아웃을 요청한다")
    public void 세션_쿠키_없이_로그아웃을_요청한다() {
        apiClient.post("/api/auth/logout");
    }

    @When("같은 세션으로 내 프로필을 조회한다")
    public void 같은_세션으로_내_프로필을_조회한다() {
        apiClient.get("/api/users/me");
    }

    @Then("응답은 SESSION 쿠키를 만료시킨다")
    public void 응답은_SESSION_쿠키를_만료시킨다() {
        apiClient
                .getLastResponse()
                .expectHeader()
                .value(
                        HttpHeaders.SET_COOKIE,
                        cookie -> assertThat(cookie)
                                .contains(SESSION_COOKIE_NAME + "=")
                                .contains("Max-Age=0"));
    }
}
