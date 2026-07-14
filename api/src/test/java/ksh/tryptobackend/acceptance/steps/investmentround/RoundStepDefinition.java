package ksh.tryptobackend.acceptance.steps.investmentround;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import ksh.tryptobackend.investmentround.adapter.out.persistence.entity.InvestmentRoundJpaEntity;
import ksh.tryptobackend.investmentround.adapter.out.persistence.repository.InvestmentRoundJpaRepository;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import ksh.tryptobackend.marketdata.adapter.out.persistence.repository.ExchangeJpaRepository;
import ksh.tryptobackend.wallet.adapter.out.persistence.entity.WalletBalanceJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.persistence.entity.WalletJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.persistence.repository.WalletBalanceJpaRepository;
import ksh.tryptobackend.wallet.adapter.out.persistence.repository.WalletJpaRepository;

public class RoundStepDefinition {

    private static final Long USER_ID = 1L;
    private static final Long UPBIT_EXCHANGE_ID = 1L;
    private static final Long KRW_COIN_ID = 1L;
    private static final BigDecimal UPBIT_SEED = new BigDecimal("5000000");

    private final CommonApiClient apiClient;
    private final ExchangeJpaRepository exchangeJpaRepository;
    private final InvestmentRoundJpaRepository investmentRoundJpaRepository;
    private final WalletJpaRepository walletJpaRepository;
    private final WalletBalanceJpaRepository walletBalanceJpaRepository;

    private Long lastRoundId;

    public RoundStepDefinition(
            CommonApiClient apiClient,
            ExchangeJpaRepository exchangeJpaRepository,
            InvestmentRoundJpaRepository investmentRoundJpaRepository,
            WalletJpaRepository walletJpaRepository,
            WalletBalanceJpaRepository walletBalanceJpaRepository) {
        this.apiClient = apiClient;
        this.exchangeJpaRepository = exchangeJpaRepository;
        this.investmentRoundJpaRepository = investmentRoundJpaRepository;
        this.walletJpaRepository = walletJpaRepository;
        this.walletBalanceJpaRepository = walletBalanceJpaRepository;
    }

    @Given("라운드용 거래소 메타데이터가 준비되어 있다")
    public void 라운드용_거래소_메타데이터가_준비되어_있다() {
        apiClient.loginAs(USER_ID);
    }

    @When("기본 라운드 시작 요청을 보낸다")
    public void 기본_라운드_시작_요청을_보낸다() {
        apiClient.post("/api/rounds", defaultRequest());
        extractRoundIdIfSuccess();
    }

    @When("거래소 {long}의 시드머니를 {long}원으로 라운드 시작 요청을 보낸다")
    public void 거래소의_시드머니를_원으로_라운드_시작_요청을_보낸다(long exchangeId, long amount) {
        Map<String, Object> request = defaultRequest();
        for (Map<String, Object> seed : getSeeds(request)) {
            if (((Number) seed.get("exchangeId")).longValue() == exchangeId) {
                seed.put("amount", amount);
            }
        }
        apiClient.post("/api/rounds", request);
    }

    @When("긴급 자금 상한을 {int}원으로 라운드 시작 요청을 보낸다")
    public void 긴급_자금_상한을_원으로_라운드_시작_요청을_보낸다(int amount) {
        Map<String, Object> request = defaultRequest();
        request.put("emergencyFundingLimit", amount);
        apiClient.post("/api/rounds", request);
    }

    @When("횟수 원칙 값을 {double}로 라운드 시작 요청을 보낸다")
    public void 횟수_원칙_값을_로_라운드_시작_요청을_보낸다(double value) {
        Map<String, Object> request = defaultRequest();
        List<Map<String, Object>> rules = getRules(request);
        rules.get(3).put("thresholdValue", value);
        apiClient.post("/api/rounds", request);
    }

    @When("투자 원칙 없이 라운드 시작 요청을 보낸다")
    public void 투자_원칙_없이_라운드_시작_요청을_보낸다() {
        Map<String, Object> request = defaultRequest();
        request.put("rules", List.of());
        apiClient.post("/api/rounds", request);
    }

    @When("활성 라운드 조회 요청을 보낸다")
    public void 활성_라운드_조회_요청을_보낸다() {
        apiClient.get("/api/rounds/active");
    }

    @When("라운드 요약 조회 요청을 보낸다")
    public void 라운드_요약_조회_요청을_보낸다() {
        apiClient.get("/api/rounds/summary");
    }

    @Then("누적 라운드 횟수는 {int}회이다")
    public void 누적_라운드_횟수는_회이다(int count) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.totalRoundCount")
                .isEqualTo(count);
    }

    @Then("시드머니는 업비트 원화 지갑에만 들어간다")
    public void 시드머니는_업비트_원화_지갑에만_들어간다() {
        for (WalletJpaEntity wallet : walletJpaRepository.findByRoundId(lastRoundId)) {
            BigDecimal krwBalance = walletBalanceJpaRepository
                    .findByWalletIdAndCoinId(wallet.getId(), KRW_COIN_ID)
                    .map(WalletBalanceJpaEntity::getAvailable)
                    .orElse(BigDecimal.ZERO);
            BigDecimal expected = UPBIT_EXCHANGE_ID.equals(wallet.getExchangeId()) ? UPBIT_SEED : BigDecimal.ZERO;

            assertThat(krwBalance).isEqualByComparingTo(expected);
        }
    }

    @Then("라운드 상태는 {string}이다")
    public void 라운드_상태는_이다(String status) {
        apiClient.getLastResponse().expectBody().jsonPath("$.data.status").isEqualTo(status);
    }

    @Then("원칙 개수는 {int}개이다")
    public void 원칙_개수는_개이다(int count) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.rules.length()")
                .isEqualTo(count);
    }

    @Then("지갑 개수는 {int}개이다")
    public void 지갑_개수는_개이다(int count) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.wallets.length()")
                .isEqualTo(count);
    }

    @When("라운드 종료 요청을 보낸다")
    public void 라운드_종료_요청을_보낸다() {
        apiClient.post("/api/rounds/" + lastRoundId + "/end");
    }

    @When("다른 사용자로 라운드 종료 요청을 보낸다")
    public void 다른_사용자로_라운드_종료_요청을_보낸다() {
        apiClient.loginAs(999L);
        apiClient.post("/api/rounds/" + lastRoundId + "/end");
    }

    @Given("파산 상태의 라운드가 존재한다")
    public void 파산_상태의_라운드가_존재한다() {
        InvestmentRoundJpaEntity entity = InvestmentRoundJpaEntity.fromDomain(InvestmentRound.reconstitute(
                null,
                null,
                USER_ID,
                1L,
                new BigDecimal("1000000"),
                new BigDecimal("500000"),
                3,
                RoundStatus.BANKRUPT,
                LocalDateTime.now(),
                null,
                List.of(),
                List.of()));
        InvestmentRoundJpaEntity saved = investmentRoundJpaRepository.save(entity);
        lastRoundId = saved.getId();
    }

    @When("존재하지 않는 라운드 종료 요청을 보낸다")
    public void 존재하지_않는_라운드_종료_요청을_보낸다() {
        apiClient.post("/api/rounds/999999/end");
    }

    private Map<String, Object> defaultRequest() {
        Map<String, Object> body = new HashMap<>();
        body.put(
                "seeds",
                List.of(seed(UPBIT_EXCHANGE_ID, UPBIT_SEED), seed(2L, BigDecimal.ZERO), seed(3L, BigDecimal.ZERO)));
        body.put("emergencyFundingLimit", new BigDecimal("500000"));
        body.put(
                "rules",
                List.of(
                        rule("LOSS_CUT", new BigDecimal("10")),
                        rule("PROFIT_TAKE", new BigDecimal("30")),
                        rule("CHASE_BUY_BAN", new BigDecimal("15")),
                        rule("AVERAGING_DOWN_LIMIT", new BigDecimal("3")),
                        rule("OVERTRADING_LIMIT", new BigDecimal("10"))));
        return body;
    }

    private Map<String, Object> seed(Long exchangeId, BigDecimal amount) {
        Map<String, Object> seed = new HashMap<>();
        seed.put("exchangeId", exchangeId);
        seed.put("amount", amount);
        return seed;
    }

    private Map<String, Object> rule(String ruleType, BigDecimal thresholdValue) {
        Map<String, Object> rule = new HashMap<>();
        rule.put("ruleType", ruleType);
        rule.put("thresholdValue", thresholdValue);
        return rule;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getSeeds(Map<String, Object> request) {
        return (List<Map<String, Object>>) request.get("seeds");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getRules(Map<String, Object> request) {
        return (List<Map<String, Object>>) request.get("rules");
    }

    @SuppressWarnings("unchecked")
    private void extractRoundIdIfSuccess() {
        Map<String, Object> body =
                apiClient.getLastResponse().expectBody(Map.class).returnResult().getResponseBody();
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        if (data != null && data.get("roundId") instanceof Number num) {
            lastRoundId = num.longValue();
        }
    }
}
