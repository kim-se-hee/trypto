package ksh.tryptobackend.acceptance.steps.user;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
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
import org.springframework.http.HttpHeaders;

public class KakaoLoginStepDefinition {

    private static final String BRAND_NEW_CODE = "kakao-brand-new";
    private static final String CODE_VERIFIER = "test-verifier";

    private final CommonApiClient apiClient;
    private final UserJpaRepository userJpaRepository;
    private final SocialAccountJpaRepository socialAccountJpaRepository;

    private Long existingUserId;
    private Long withdrawnUserId;
    private long userCountBeforeLogin;

    public KakaoLoginStepDefinition(
            CommonApiClient apiClient,
            UserJpaRepository userJpaRepository,
            SocialAccountJpaRepository socialAccountJpaRepository) {
        this.apiClient = apiClient;
        this.userJpaRepository = userJpaRepository;
        this.socialAccountJpaRepository = socialAccountJpaRepository;
    }

    @Given("카카오 신원 {string}에 연결된 회원이 존재한다")
    public void 카카오_신원에_연결된_회원이_존재한다(String providerId) {
        SocialAccountJpaEntity account = saveDisconnectedAccount(providerId);
        UserJpaEntity user = userJpaRepository.save(
                UserJpaEntity.fromDomain(User.create(account.getId(), "기존회원" + providerId, true, LocalDateTime.now())));
        account.connectTo(user.getId());
        socialAccountJpaRepository.save(account);
        existingUserId = user.getId();
    }

    @Given("카카오 신원 {string}로 가입한 회원이 탈퇴하여 연결이 해제되어 있다")
    public void 카카오_신원으로_가입한_회원이_탈퇴하여_연결이_해제되어_있다(String providerId) {
        SocialAccountJpaEntity account = saveDisconnectedAccount(providerId);
        UserJpaEntity withdrawn = userJpaRepository.save(UserJpaEntity.fromDomain(User.reconstitute(
                null,
                null,
                account.getId(),
                "탈퇴한사용자" + providerId,
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now())));
        withdrawnUserId = withdrawn.getId();
    }

    @Given("그 회원의 탈퇴 시점으로부터 {int}일이 지난 시점이다")
    public void 그_회원의_탈퇴_시점으로부터_일이_지난_시점이다(int days) {
        UserJpaEntity entity = userJpaRepository.findById(withdrawnUserId).orElseThrow();
        User updated = User.reconstitute(
                entity.getId(),
                entity.getVersion(),
                entity.getSocialAccountId(),
                entity.getNickname(),
                entity.isPortfolioPublic(),
                LocalDateTime.now().minusDays(days),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
        userJpaRepository.saveAndFlush(UserJpaEntity.fromDomain(updated));
    }

    @Given("카카오 신원 {string}로 로그인에 성공한다")
    public void 카카오_신원으로_로그인에_성공한다(String providerId) {
        login(providerId);
        apiClient.getLastResponse().expectStatus().isEqualTo(200);
        apiClient.adoptSessionFromLastResponse();
    }

    @When("연결된 회원이 없는 카카오 신원으로 로그인을 요청한다")
    public void 연결된_회원이_없는_카카오_신원으로_로그인을_요청한다() {
        login(BRAND_NEW_CODE);
    }

    @When("카카오 신원 {string}로 로그인을 요청한다")
    public void 카카오_신원으로_로그인을_요청한다(String providerId) {
        login(providerId);
    }

    @When("발급받은 세션으로 내 프로필을 조회한다")
    public void 발급받은_세션으로_내_프로필을_조회한다() {
        apiClient.get("/api/users/me");
    }

    @Then("응답의 newUser는 {word}이다")
    public void 응답의_newUser는_이다(String expected) {
        apiClient.getLastResponse().expectBody().jsonPath("$.data.newUser").isEqualTo(Boolean.parseBoolean(expected));
    }

    @Then("응답에 닉네임이 부여되어 있다")
    public void 응답에_닉네임이_부여되어_있다() {
        apiClient.getLastResponse().expectBody().jsonPath("$.data.nickname").isNotEmpty();
    }

    @Then("응답은 SESSION 쿠키를 발급한다")
    public void 응답은_SESSION_쿠키를_발급한다() {
        apiClient
                .getLastResponse()
                .expectHeader()
                .value(
                        HttpHeaders.SET_COOKIE,
                        cookie -> assertThat(cookie).contains("SESSION=").doesNotContain("Max-Age=0"));
    }

    @Then("응답의 userId는 기존 회원과 같다")
    public void 응답의_userId는_기존_회원과_같다() {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.userId")
                .value(id ->
                        assertThat(new BigDecimal(id.toString()).longValue()).isEqualTo(existingUserId));
    }

    @Then("응답의 userId는 이전 탈퇴 회원과 다르다")
    public void 응답의_userId는_이전_탈퇴_회원과_다르다() {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.userId")
                .value(id ->
                        assertThat(new BigDecimal(id.toString()).longValue()).isNotEqualTo(withdrawnUserId));
    }

    @Then("새로운 회원이 생성되지 않는다")
    public void 새로운_회원이_생성되지_않는다() {
        assertThat(userJpaRepository.count()).isEqualTo(userCountBeforeLogin);
    }

    private SocialAccountJpaEntity saveDisconnectedAccount(String providerId) {
        return socialAccountJpaRepository.save(SocialAccountJpaEntity.fromDomain(
                SocialAccount.register(SocialIdentity.of(Provider.KAKAO, providerId), LocalDateTime.now())));
    }

    private void login(String code) {
        userCountBeforeLogin = userJpaRepository.count();
        apiClient.post("/api/auth/kakao/login", Map.of("code", code, "codeVerifier", CODE_VERIFIER));
    }
}
