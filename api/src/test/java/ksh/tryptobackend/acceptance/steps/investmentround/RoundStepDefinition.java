package ksh.tryptobackend.acceptance.steps.investmentround;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ksh.tryptobackend.acceptance.mock.MockLivePriceAdapter;
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
    private static final BigDecimal UPBIT_SEED = new BigDecimal("5000000");
    // seed-data.sql 의 거래소별 기축통화 코인. 업비트(1)·빗썸(2)=KRW(1), 바이낸스(3)=USDT(4)
    private static final Map<Long, Long> BASE_CURRENCY_COIN_BY_EXCHANGE = Map.of(1L, 1L, 2L, 1L, 3L, 4L);
    // 빗썸 USDT 마켓. USDT 시드의 원화 환산 시세 소스다
    private static final Long USDT_KRW_EXCHANGE_COIN_ID = 13L;

    private final CommonApiClient apiClient;
    private final ExchangeJpaRepository exchangeJpaRepository;
    private final InvestmentRoundJpaRepository investmentRoundJpaRepository;
    private final WalletJpaRepository walletJpaRepository;
    private final WalletBalanceJpaRepository walletBalanceJpaRepository;
    private final MockLivePriceAdapter livePriceAdapter;

    private Long lastRoundId;
    private Map<Long, BigDecimal> lastRequestedSeeds;

    public RoundStepDefinition(
            CommonApiClient apiClient,
            ExchangeJpaRepository exchangeJpaRepository,
            InvestmentRoundJpaRepository investmentRoundJpaRepository,
            WalletJpaRepository walletJpaRepository,
            WalletBalanceJpaRepository walletBalanceJpaRepository,
            MockLivePriceAdapter livePriceAdapter) {
        this.apiClient = apiClient;
        this.exchangeJpaRepository = exchangeJpaRepository;
        this.investmentRoundJpaRepository = investmentRoundJpaRepository;
        this.walletJpaRepository = walletJpaRepository;
        this.walletBalanceJpaRepository = walletBalanceJpaRepository;
        this.livePriceAdapter = livePriceAdapter;
    }

    @Given("라운드용 거래소 메타데이터가 준비되어 있다")
    public void 라운드용_거래소_메타데이터가_준비되어_있다() {
        apiClient.loginAs(USER_ID);
    }

    @Given("USDT 원화 시세는 {long}원이다")
    public void USDT_원화_시세는_원이다(long price) {
        livePriceAdapter.setPrice(USDT_KRW_EXCHANGE_COIN_ID, new BigDecimal(price));
    }

    @When("기본 라운드 시작 요청을 보낸다")
    public void 기본_라운드_시작_요청을_보낸다() {
        startRound(defaultRequest());
    }

    @When("다음 시드머니로 라운드 시작 요청을 보낸다")
    public void 다음_시드머니로_라운드_시작_요청을_보낸다(DataTable table) {
        Map<Long, BigDecimal> amounts = new LinkedHashMap<>();
        for (long exchangeId = 1; exchangeId <= 3; exchangeId++) {
            amounts.put(exchangeId, BigDecimal.ZERO);
        }
        for (Map<String, String> row : table.asMaps()) {
            amounts.put(Long.valueOf(row.get("거래소")), new BigDecimal(row.get("금액")));
        }

        Map<String, Object> request = defaultRequest();
        request.put(
                "seeds",
                amounts.entrySet().stream()
                        .map(entry -> seed(entry.getKey(), entry.getValue()))
                        .toList());
        startRound(request);
    }

    @When("거래소 {long}의 시드머니를 {long}원으로 라운드 시작 요청을 보낸다")
    public void 거래소의_시드머니를_원으로_라운드_시작_요청을_보낸다(long exchangeId, long amount) {
        overrideSeedAndStart(exchangeId, amount);
    }

    @When("거래소 {long}의 시드머니를 {long} USDT로 라운드 시작 요청을 보낸다")
    public void 거래소의_시드머니를_USDT로_라운드_시작_요청을_보낸다(long exchangeId, long amount) {
        overrideSeedAndStart(exchangeId, amount);
    }

    @When("거래소 {long}의 시드머니를 중복으로 담아 라운드 시작 요청을 보낸다")
    public void 거래소의_시드머니를_중복으로_담아_라운드_시작_요청을_보낸다(long exchangeId) {
        Map<String, Object> request = defaultRequest();
        List<Map<String, Object>> seeds = new ArrayList<>(getSeeds(request));
        seeds.add(seed(exchangeId, new BigDecimal("3000000")));
        request.put("seeds", seeds);
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

    @Then("각 거래소 지갑의 기축통화 잔고는 입력한 시드머니와 일치한다")
    public void 각_거래소_지갑의_기축통화_잔고는_입력한_시드머니와_일치한다() {
        List<WalletJpaEntity> wallets = walletJpaRepository.findByRoundId(lastRoundId);
        assertThat(wallets).hasSize(lastRequestedSeeds.size());

        for (WalletJpaEntity wallet : wallets) {
            Long baseCurrencyCoinId = BASE_CURRENCY_COIN_BY_EXCHANGE.get(wallet.getExchangeId());
            BigDecimal balance = walletBalanceJpaRepository
                    .findByWalletIdAndCoinId(wallet.getId(), baseCurrencyCoinId)
                    .map(WalletBalanceJpaEntity::getAvailable)
                    .orElse(BigDecimal.ZERO);

            assertThat(balance).isEqualByComparingTo(lastRequestedSeeds.get(wallet.getExchangeId()));
        }
    }

    @Then("시드머니 총액은 {long}원이다")
    public void 시드머니_총액은_원이다(long total) {
        apiClient.getLastResponse().expectBody().jsonPath("$.data.initialSeed").isEqualTo(total);
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

    private void startRound(Map<String, Object> request) {
        lastRequestedSeeds = new HashMap<>();
        for (Map<String, Object> seed : getSeeds(request)) {
            lastRequestedSeeds.put(((Number) seed.get("exchangeId")).longValue(), (BigDecimal) seed.get("amount"));
        }
        apiClient.post("/api/rounds", request);
        extractRoundIdIfSuccess();
    }

    private void overrideSeedAndStart(long exchangeId, long amount) {
        Map<String, Object> request = defaultRequest();
        for (Map<String, Object> seed : getSeeds(request)) {
            if (((Number) seed.get("exchangeId")).longValue() == exchangeId) {
                seed.put("amount", new BigDecimal(amount));
            }
        }
        startRound(request);
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
