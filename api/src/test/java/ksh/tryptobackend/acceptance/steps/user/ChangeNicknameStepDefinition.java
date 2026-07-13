package ksh.tryptobackend.acceptance.steps.user;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.LocalDateTime;
import java.util.Map;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import ksh.tryptobackend.user.adapter.out.persistence.entity.SocialAccountJpaEntity;
import ksh.tryptobackend.user.adapter.out.persistence.entity.UserJpaEntity;
import ksh.tryptobackend.user.adapter.out.persistence.repository.SocialAccountJpaRepository;
import ksh.tryptobackend.user.adapter.out.persistence.repository.UserJpaRepository;
import ksh.tryptobackend.user.domain.model.SocialAccount;
import ksh.tryptobackend.user.domain.model.User;
import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;

public class ChangeNicknameStepDefinition {

    private final CommonApiClient apiClient;
    private final UserJpaRepository userJpaRepository;
    private final SocialAccountJpaRepository socialAccountJpaRepository;

    private Long userId;

    public ChangeNicknameStepDefinition(
            CommonApiClient apiClient,
            UserJpaRepository userJpaRepository,
            SocialAccountJpaRepository socialAccountJpaRepository) {
        this.apiClient = apiClient;
        this.userJpaRepository = userJpaRepository;
        this.socialAccountJpaRepository = socialAccountJpaRepository;
    }

    @Given("닉네임이 {string}인 사용자가 존재한다")
    public void 닉네임이_인_사용자가_존재한다(String nickname) {
        userId = createUser(nickname);
    }

    @Given("닉네임이 {string}인 다른 사용자가 존재한다")
    public void 닉네임이_인_다른_사용자가_존재한다(String nickname) {
        createUser(nickname);
    }

    private Long createUser(String nickname) {
        SocialAccountJpaEntity account = socialAccountJpaRepository.save(SocialAccountJpaEntity.fromDomain(
                SocialAccount.register(SocialIdentity.of(Provider.KAKAO, "test-" + nickname), LocalDateTime.now())));
        UserJpaEntity saved = userJpaRepository.save(
                UserJpaEntity.fromDomain(User.create(account.getId(), nickname, LocalDateTime.now())));
        return saved.getId();
    }

    @When("닉네임을 {string}로 변경 요청한다")
    public void 닉네임을_로_변경_요청한다(String nickname) {
        apiClient.loginAs(userId);
        apiClient.put("/api/users/me/nickname", Map.of("nickname", nickname));
    }

    @When("닉네임을 {string}으로 변경 요청한다")
    public void 닉네임을_으로_변경_요청한다(String nickname) {
        apiClient.loginAs(userId);
        apiClient.put("/api/users/me/nickname", Map.of("nickname", nickname));
    }

    @When("존재하지 않는 사용자의 닉네임을 {string}로 변경 요청한다")
    public void 존재하지_않는_사용자의_닉네임을_로_변경_요청한다(String nickname) {
        apiClient.loginAs(999999L);
        apiClient.put("/api/users/me/nickname", Map.of("nickname", nickname));
    }

    @When("존재하지 않는 사용자의 닉네임을 {string}으로 변경 요청한다")
    public void 존재하지_않는_사용자의_닉네임을_으로_변경_요청한다(String nickname) {
        apiClient.loginAs(999999L);
        apiClient.put("/api/users/me/nickname", Map.of("nickname", nickname));
    }

    @Then("응답 코드는 {string}이다")
    public void 응답_코드는_이다(String code) {
        apiClient.getLastResponse().expectBody().jsonPath("$.code").isEqualTo(code);
    }

    @Then("응답의 닉네임은 {string}이다")
    public void 응답의_닉네임은_이다(String nickname) {
        apiClient.getLastResponse().expectBody().jsonPath("$.data.nickname").isEqualTo(nickname);
    }
}
