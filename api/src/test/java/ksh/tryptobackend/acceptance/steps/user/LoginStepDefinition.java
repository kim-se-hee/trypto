package ksh.tryptobackend.acceptance.steps.user;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import ksh.tryptobackend.acceptance.mock.MockSocialAuthenticator;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import ksh.tryptobackend.user.adapter.out.persistence.entity.SocialAccountJpaEntity;
import ksh.tryptobackend.user.adapter.out.persistence.entity.UserJpaEntity;
import ksh.tryptobackend.user.adapter.out.persistence.repository.SocialAccountJpaRepository;
import ksh.tryptobackend.user.adapter.out.persistence.repository.UserJpaRepository;
import ksh.tryptobackend.user.domain.model.SocialAccount;
import ksh.tryptobackend.user.domain.model.User;
import ksh.tryptobackend.user.domain.vo.ClientType;
import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import org.springframework.http.HttpHeaders;

public class LoginStepDefinition {

    private static final String BRAND_NEW_CODE = "brand-new";
    private static final String CODE_VERIFIER = "test-verifier";

    private final CommonApiClient apiClient;
    private final UserJpaRepository userJpaRepository;
    private final SocialAccountJpaRepository socialAccountJpaRepository;
    private final MockSocialAuthenticator socialAuthenticator;

    private Long existingUserId;
    private Long withdrawnUserId;
    private long userCountBeforeLogin;

    public LoginStepDefinition(
            CommonApiClient apiClient,
            UserJpaRepository userJpaRepository,
            SocialAccountJpaRepository socialAccountJpaRepository,
            MockSocialAuthenticator socialAuthenticator) {
        this.apiClient = apiClient;
        this.userJpaRepository = userJpaRepository;
        this.socialAccountJpaRepository = socialAccountJpaRepository;
        this.socialAuthenticator = socialAuthenticator;
    }

    @Given("{word} 신원 {string}에 연결된 회원이 존재한다")
    public void 신원에_연결된_회원이_존재한다(String providerLabel, String providerId) {
        Provider provider = providerOf(providerLabel);
        SocialAccountJpaEntity account = saveDisconnectedAccount(provider, providerId);
        UserJpaEntity user = userJpaRepository.save(
                UserJpaEntity.fromDomain(User.create(account.getId(), "기존회원" + providerId, LocalDateTime.now())));
        account.connectTo(user.getId());
        socialAccountJpaRepository.save(account);
        existingUserId = user.getId();
    }

    @Given("{word} 신원 {string}로 로그인한 회원이 탈퇴한다")
    public void 신원으로_로그인한_회원이_탈퇴한다(String providerLabel, String providerId) {
        login(providerOf(providerLabel), providerId);
        apiClient.getLastResponse().expectStatus().isEqualTo(200);
        apiClient.adoptSessionFromLastResponse();
        withdrawnUserId = extractUserId();

        apiClient.delete("/api/users/me");
        apiClient.getLastResponse().expectStatus().isEqualTo(200);
    }

    @Given("그 회원의 탈퇴 시점으로부터 {int}일이 지난 시점이다")
    public void 그_회원의_탈퇴_시점으로부터_일이_지난_시점이다(int days) {
        UserJpaEntity entity = userJpaRepository.findById(withdrawnUserId).orElseThrow();
        User updated = User.reconstitute(
                entity.getId(),
                entity.getVersion(),
                entity.getSocialAccountId(),
                entity.getNickname(),
                LocalDateTime.now().minusDays(days),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
        userJpaRepository.saveAndFlush(UserJpaEntity.fromDomain(updated));
    }

    @Given("{word} 신원 {string}로 로그인에 성공한다")
    public void 신원으로_로그인에_성공한다(String providerLabel, String providerId) {
        login(providerOf(providerLabel), providerId);
        apiClient.getLastResponse().expectStatus().isEqualTo(200);
        apiClient.adoptSessionFromLastResponse();
    }

    @When("연결된 회원이 없는 {word} 신원으로 로그인을 요청한다")
    public void 연결된_회원이_없는_신원으로_로그인을_요청한다(String providerLabel) {
        login(providerOf(providerLabel), BRAND_NEW_CODE);
    }

    @When("{word} 신원 {string}로 로그인을 요청한다")
    public void 신원으로_로그인을_요청한다(String providerLabel, String providerId) {
        login(providerOf(providerLabel), providerId);
    }

    @When("지원하지 않는 제공자 {string}로 로그인을 요청한다")
    public void 지원하지_않는_제공자로_로그인을_요청한다(String provider) {
        userCountBeforeLogin = userJpaRepository.count();
        apiClient.post(
                "/api/auth/" + provider + "/login", Map.of("code", BRAND_NEW_CODE, "codeVerifier", CODE_VERIFIER));
    }

    @When("클라이언트 유형 {string}로 카카오 로그인을 요청한다")
    public void 클라이언트_유형으로_카카오_로그인을_요청한다(String clientType) {
        userCountBeforeLogin = userJpaRepository.count();
        apiClient.post(
                "/api/auth/kakao/login",
                Map.of("code", BRAND_NEW_CODE, "codeVerifier", CODE_VERIFIER, "clientType", clientType));
    }

    @Then("인증에 사용된 클라이언트 유형은 {word}이다")
    public void 인증에_사용된_클라이언트_유형은_이다(String expected) {
        assertThat(socialAuthenticator.getLastClientType()).isEqualTo(ClientType.valueOf(expected));
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

    @Then("응답의 userId는 기존 회원과 다르다")
    public void 응답의_userId는_기존_회원과_다르다() {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.userId")
                .value(id ->
                        assertThat(new BigDecimal(id.toString()).longValue()).isNotEqualTo(existingUserId));
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

    private static Provider providerOf(String label) {
        return switch (label) {
            case "카카오" -> Provider.KAKAO;
            case "구글" -> Provider.GOOGLE;
            default -> throw new IllegalArgumentException("지원하지 않는 제공자 표기: " + label);
        };
    }

    private Long extractUserId() {
        AtomicLong userId = new AtomicLong();
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.userId")
                .value(id -> userId.set(new BigDecimal(id.toString()).longValue()));
        return userId.get();
    }

    private SocialAccountJpaEntity saveDisconnectedAccount(Provider provider, String providerId) {
        return socialAccountJpaRepository.save(SocialAccountJpaEntity.fromDomain(
                SocialAccount.register(SocialIdentity.of(provider, providerId), LocalDateTime.now())));
    }

    private void login(Provider provider, String code) {
        userCountBeforeLogin = userJpaRepository.count();
        apiClient.post(
                "/api/auth/" + provider.name().toLowerCase(Locale.ROOT) + "/login",
                Map.of("code", code, "codeVerifier", CODE_VERIFIER));
    }
}
