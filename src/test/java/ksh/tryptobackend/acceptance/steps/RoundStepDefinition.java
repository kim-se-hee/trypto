package ksh.tryptobackend.acceptance.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import ksh.tryptobackend.investmentround.adapter.out.entity.InvestmentRoundJpaEntity;
import ksh.tryptobackend.investmentround.adapter.out.repository.InvestmentRoundJpaRepository;
import ksh.tryptobackend.investmentround.adapter.out.repository.InvestmentRuleJpaRepository;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeJpaRepository;
import ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletJpaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoundStepDefinition {

    private static final Long USER_ID = 1L;

    private final CommonApiClient apiClient;
    private final ExchangeJpaRepository exchangeJpaRepository;
    private final InvestmentRoundJpaRepository investmentRoundJpaRepository;
    private final InvestmentRuleJpaRepository investmentRuleJpaRepository;
    private final WalletJpaRepository walletJpaRepository;

    private Long lastRoundId;

    public RoundStepDefinition(
        CommonApiClient apiClient,
        ExchangeJpaRepository exchangeJpaRepository,
        InvestmentRoundJpaRepository investmentRoundJpaRepository,
        InvestmentRuleJpaRepository investmentRuleJpaRepository,
        WalletJpaRepository walletJpaRepository
    ) {
        this.apiClient = apiClient;
        this.exchangeJpaRepository = exchangeJpaRepository;
        this.investmentRoundJpaRepository = investmentRoundJpaRepository;
        this.investmentRuleJpaRepository = investmentRuleJpaRepository;
        this.walletJpaRepository = walletJpaRepository;
    }

    @Before
    public void setUp() {
        investmentRuleJpaRepository.deleteAllInBatch();
        walletJpaRepository.deleteAllInBatch();
        investmentRoundJpaRepository.deleteAllInBatch();
        exchangeJpaRepository.deleteAllInBatch();
        lastRoundId = null;
    }

    @Given("라운드용 거래소 메타데이터가 준비되어 있다")
    public void 라운드용_거래소_메타데이터가_준비되어_있다() {
        exchangeJpaRepository.saveAll(List.of(
            new ExchangeJpaEntity(1L, "UPBIT", ExchangeMarketType.DOMESTIC, 1L),
            new ExchangeJpaEntity(2L, "BITHUMB", ExchangeMarketType.DOMESTIC, 1L),
            new ExchangeJpaEntity(3L, "BINANCE", ExchangeMarketType.OVERSEAS, 3L)
        ));
    }

    @When("기본 라운드 시작 요청을 보낸다")
    public void 기본_라운드_시작_요청을_보낸다() {
        apiClient.post("/api/rounds", defaultRequest());
        extractRoundIdIfSuccess();
    }

    @When("국내 거래소 시드머니를 {long}원으로 라운드 시작 요청을 보낸다")
    public void 국내_거래소_시드머니를_원으로_라운드_시작_요청을_보낸다(long amount) {
        Map<String, Object> request = defaultRequest();
        List<Map<String, Object>> seeds = getSeeds(request);
        seeds.get(0).put("amount", amount);
        apiClient.post("/api/rounds", request);
    }

    @When("해외 거래소 시드머니를 {int} USDT로 라운드 시작 요청을 보낸다")
    public void 해외_거래소_시드머니를_usdt로_라운드_시작_요청을_보낸다(int amount) {
        Map<String, Object> request = defaultRequest();
        List<Map<String, Object>> seeds = getSeeds(request);
        seeds.get(2).put("amount", amount);
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
        apiClient.get("/api/rounds/active?userId=" + USER_ID);
    }

    @When("거래소 {long}의 시드머니를 0으로 라운드 시작 요청을 보낸다")
    public void 거래소_의_시드머니를_0으로_라운드_시작_요청을_보낸다(long exchangeId) {
        Map<String, Object> request = defaultRequest();
        List<Map<String, Object>> seeds = getSeeds(request);
        for (Map<String, Object> seed : seeds) {
            if (((Number) seed.get("exchangeId")).longValue() == exchangeId) {
                seed.put("amount", 0);
            }
        }
        apiClient.post("/api/rounds", request);
    }

    @Then("라운드 상태는 {string}이다")
    public void 라운드_상태는_이다(String status) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.status").isEqualTo(status);
    }

    @Then("원칙 개수는 {int}개이다")
    public void 원칙_개수는_개이다(int count) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.rules.length()").isEqualTo(count);
    }

    @When("라운드 종료 요청을 보낸다")
    public void 라운드_종료_요청을_보낸다() {
        Map<String, Object> body = Map.of("userId", USER_ID);
        apiClient.post("/api/rounds/" + lastRoundId + "/end", body);
    }

    @When("다른 사용자로 라운드 종료 요청을 보낸다")
    public void 다른_사용자로_라운드_종료_요청을_보낸다() {
        Map<String, Object> body = Map.of("userId", 999L);
        apiClient.post("/api/rounds/" + lastRoundId + "/end", body);
    }

    @Given("파산 상태의 라운드가 존재한다")
    public void 파산_상태의_라운드가_존재한다() {
        InvestmentRoundJpaEntity entity = InvestmentRoundJpaEntity.fromDomain(
            InvestmentRound.builder()
                .userId(USER_ID)
                .roundNumber(1L)
                .initialSeed(new BigDecimal("1000000"))
                .emergencyFundingLimit(new BigDecimal("500000"))
                .emergencyChargeCount(3)
                .status(RoundStatus.BANKRUPT)
                .startedAt(LocalDateTime.now())
                .build());
        InvestmentRoundJpaEntity saved = investmentRoundJpaRepository.save(entity);
        lastRoundId = saved.getId();
    }

    @When("존재하지 않는 라운드 종료 요청을 보낸다")
    public void 존재하지_않는_라운드_종료_요청을_보낸다() {
        Map<String, Object> body = Map.of("userId", USER_ID);
        apiClient.post("/api/rounds/999999/end", body);
    }

    private Map<String, Object> defaultRequest() {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", USER_ID);
        body.put("seeds", List.of(
            seed(1L, new BigDecimal("5000000")),
            seed(2L, new BigDecimal("3000000")),
            seed(3L, new BigDecimal("100"))
        ));
        body.put("emergencyFundingLimit", new BigDecimal("500000"));
        body.put("rules", List.of(
            rule("LOSS_CUT", new BigDecimal("10")),
            rule("PROFIT_TAKE", new BigDecimal("30")),
            rule("CHASE_BUY_BAN", new BigDecimal("15")),
            rule("AVERAGING_DOWN_LIMIT", new BigDecimal("3")),
            rule("OVERTRADING_LIMIT", new BigDecimal("10"))
        ));
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
        Map<String, Object> body = apiClient.getLastResponse()
            .expectBody(Map.class)
            .returnResult()
            .getResponseBody();
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        if (data != null && data.get("roundId") instanceof Number num) {
            lastRoundId = num.longValue();
        }
    }
}
