package ksh.tryptobackend.acceptance.steps;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import ksh.tryptobackend.investmentround.adapter.out.repository.EmergencyFundingJpaRepository;
import ksh.tryptobackend.investmentround.adapter.out.repository.InvestmentRoundJpaRepository;
import ksh.tryptobackend.investmentround.adapter.out.repository.InvestmentRuleJpaRepository;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeJpaRepository;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletBalanceJpaRepository;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletJpaRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType.DOMESTIC;

public class EmergencyFundingStepDefinition {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long EXCHANGE_ID = 1L;

    private final CommonApiClient apiClient;
    private final ExchangeJpaRepository exchangeJpaRepository;
    private final InvestmentRoundJpaRepository investmentRoundJpaRepository;
    private final InvestmentRuleJpaRepository investmentRuleJpaRepository;
    private final EmergencyFundingJpaRepository emergencyFundingJpaRepository;
    private final WalletJpaRepository walletJpaRepository;
    private final WalletBalanceJpaRepository walletBalanceJpaRepository;

    private Long activeRoundId;
    private Long disabledFundingRoundId;
    private UUID lastIdempotencyKey;

    public EmergencyFundingStepDefinition(
        CommonApiClient apiClient,
        ExchangeJpaRepository exchangeJpaRepository,
        InvestmentRoundJpaRepository investmentRoundJpaRepository,
        InvestmentRuleJpaRepository investmentRuleJpaRepository,
        EmergencyFundingJpaRepository emergencyFundingJpaRepository,
        WalletJpaRepository walletJpaRepository,
        WalletBalanceJpaRepository walletBalanceJpaRepository
    ) {
        this.apiClient = apiClient;
        this.exchangeJpaRepository = exchangeJpaRepository;
        this.investmentRoundJpaRepository = investmentRoundJpaRepository;
        this.investmentRuleJpaRepository = investmentRuleJpaRepository;
        this.emergencyFundingJpaRepository = emergencyFundingJpaRepository;
        this.walletJpaRepository = walletJpaRepository;
        this.walletBalanceJpaRepository = walletBalanceJpaRepository;
    }

    @Before("@emergency-funding")
    public void setUp() {
        emergencyFundingJpaRepository.deleteAllInBatch();
        investmentRuleJpaRepository.deleteAllInBatch();
        walletBalanceJpaRepository.deleteAllInBatch();
        walletJpaRepository.deleteAllInBatch();
        investmentRoundJpaRepository.deleteAllInBatch();
        exchangeJpaRepository.deleteAllInBatch();
        activeRoundId = null;
        disabledFundingRoundId = null;
        lastIdempotencyKey = null;
    }

    @Given("긴급자금용 거래소 메타데이터가 준비되어 있다")
    public void 긴급자금용_거래소_메타데이터가_준비되어_있다() {
        exchangeJpaRepository.saveAll(List.of(
            new ExchangeJpaEntity(1L, "UPBIT", DOMESTIC, 1L, new BigDecimal("0.0005")),
            new ExchangeJpaEntity(2L, "BITHUMB", DOMESTIC, 1L, new BigDecimal("0.0005"))
        ));
    }

    @Given("활성 라운드가 존재한다")
    public void 활성_라운드가_존재한다() {
        apiClient.post("/api/rounds", roundRequest(USER_ID, new BigDecimal("500000")));
        activeRoundId = extractRoundId();
    }

    @Given("긴급 자금이 비활성인 라운드가 존재한다")
    public void 긴급_자금이_비활성인_라운드가_존재한다() {
        emergencyFundingJpaRepository.deleteAllInBatch();
        investmentRuleJpaRepository.deleteAllInBatch();
        walletBalanceJpaRepository.deleteAllInBatch();
        walletJpaRepository.deleteAllInBatch();
        investmentRoundJpaRepository.deleteAllInBatch();

        apiClient.post("/api/rounds", roundRequest(USER_ID, BigDecimal.ZERO));
        disabledFundingRoundId = extractRoundId();
    }

    @When("거래소 {long}에 {long}원 긴급 자금 충전을 요청한다")
    public void 거래소에_원_긴급_자금_충전을_요청한다(long exchangeId, long amount) {
        lastIdempotencyKey = UUID.randomUUID();
        apiClient.post(
            "/api/rounds/" + activeRoundId + "/emergency-funding",
            fundingRequest(USER_ID, exchangeId, new BigDecimal(amount), lastIdempotencyKey));
    }

    @When("동일 멱등 키로 거래소 {long}에 {long}원 긴급 자금 충전을 요청한다")
    public void 동일_멱등_키로_거래소에_원_긴급_자금_충전을_요청한다(long exchangeId, long amount) {
        apiClient.post(
            "/api/rounds/" + activeRoundId + "/emergency-funding",
            fundingRequest(USER_ID, exchangeId, new BigDecimal(amount), lastIdempotencyKey));
    }

    @When("존재하지 않는 라운드에 긴급 자금 충전을 요청한다")
    public void 존재하지_않는_라운드에_긴급_자금_충전을_요청한다() {
        apiClient.post(
            "/api/rounds/999999/emergency-funding",
            fundingRequest(USER_ID, EXCHANGE_ID, new BigDecimal("100000"), UUID.randomUUID()));
    }

    @When("다른 사용자로 긴급 자금 충전을 요청한다")
    public void 다른_사용자로_긴급_자금_충전을_요청한다() {
        apiClient.post(
            "/api/rounds/" + activeRoundId + "/emergency-funding",
            fundingRequest(OTHER_USER_ID, EXCHANGE_ID, new BigDecimal("100000"), UUID.randomUUID()));
    }

    @When("지갑이 없는 거래소에 긴급 자금 충전을 요청한다")
    public void 지갑이_없는_거래소에_긴급_자금_충전을_요청한다() {
        Long nonExistentExchangeId = 999L;
        apiClient.post(
            "/api/rounds/" + activeRoundId + "/emergency-funding",
            fundingRequest(USER_ID, nonExistentExchangeId, new BigDecimal("100000"), UUID.randomUUID()));
    }

    @When("비활성 라운드에 긴급 자금 충전을 요청한다")
    public void 비활성_라운드에_긴급_자금_충전을_요청한다() {
        apiClient.post(
            "/api/rounds/" + disabledFundingRoundId + "/emergency-funding",
            fundingRequest(USER_ID, EXCHANGE_ID, new BigDecimal("100000"), UUID.randomUUID()));
    }

    @Then("충전 금액은 {long}이다")
    public void 충전_금액은_이다(long amount) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.chargedAmount").isEqualTo(amount);
    }

    @Then("잔여 충전 횟수는 {int}이다")
    public void 잔여_충전_횟수는_이다(int count) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.remainingChargeCount").isEqualTo(count);
    }

    private Long extractRoundId() {
        byte[] body = apiClient.getLastResponse()
            .expectBody().returnResult().getResponseBody();
        return ((Number) JsonPath.read(new String(body), "$.data.roundId")).longValue();
    }

    private Map<String, Object> roundRequest(Long userId, BigDecimal emergencyFundingLimit) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("seeds", List.of(seed(1L, new BigDecimal("5000000")), seed(2L, new BigDecimal("3000000"))));
        body.put("emergencyFundingLimit", emergencyFundingLimit);
        body.put("rules", List.of());
        return body;
    }

    private Map<String, Object> seed(Long exchangeId, BigDecimal amount) {
        Map<String, Object> seed = new HashMap<>();
        seed.put("exchangeId", exchangeId);
        seed.put("amount", amount);
        return seed;
    }

    private Map<String, Object> fundingRequest(Long userId, Long exchangeId, BigDecimal amount, UUID idempotencyKey) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("exchangeId", exchangeId);
        body.put("amount", amount);
        body.put("idempotencyKey", idempotencyKey.toString());
        return body;
    }
}
