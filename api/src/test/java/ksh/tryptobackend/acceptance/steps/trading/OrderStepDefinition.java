package ksh.tryptobackend.acceptance.steps.trading;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import ksh.tryptobackend.acceptance.mock.MockHoldingAdapter;
import ksh.tryptobackend.acceptance.mock.MockLivePriceAdapter;
import ksh.tryptobackend.acceptance.mock.MockPriceChangeRateAdapter;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeJpaRepository;
import ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType;
import ksh.tryptobackend.trading.adapter.out.repository.OrderJpaRepository;
import ksh.tryptobackend.wallet.adapter.out.entity.WalletBalanceJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletBalanceJpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;

public class OrderStepDefinition {

    private static final Long USER_ID = 1L;
    private static final Long ROUND_ID = 1L;
    private static final Long WALLET_ID = 1L;
    private static final Long EXCHANGE_ID = 1L;
    private static final Long EXCHANGE_COIN_ID = 10L;
    private static final Long KRW_COIN_ID = 1L;
    private static final Long BTC_COIN_ID = 2L;
    private final CommonApiClient apiClient;
    private final MockLivePriceAdapter livePriceAdapter;
    private final MockHoldingAdapter holdingAdapter;
    private final MockPriceChangeRateAdapter priceChangeRateAdapter;
    private final OrderJpaRepository orderJpaRepository;
    private final WalletBalanceJpaRepository walletBalanceJpaRepository;
    private final ExchangeJpaRepository exchangeJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    private Long lastOrderId;
    private String savedIdempotencyKey;
    private Long firstOrderId;

    public OrderStepDefinition(
            CommonApiClient apiClient,
            MockLivePriceAdapter livePriceAdapter,
            MockHoldingAdapter holdingAdapter,
            MockPriceChangeRateAdapter priceChangeRateAdapter,
            OrderJpaRepository orderJpaRepository,
            WalletBalanceJpaRepository walletBalanceJpaRepository,
            ExchangeJpaRepository exchangeJpaRepository,
            JdbcTemplate jdbcTemplate) {
        this.apiClient = apiClient;
        this.livePriceAdapter = livePriceAdapter;
        this.holdingAdapter = holdingAdapter;
        this.priceChangeRateAdapter = priceChangeRateAdapter;
        this.orderJpaRepository = orderJpaRepository;
        this.walletBalanceJpaRepository = walletBalanceJpaRepository;
        this.exchangeJpaRepository = exchangeJpaRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Given("업비트 거래소가 등록되어 있다")
    public void 업비트_거래소가_등록되어_있다() {
        // seed-data.sql 에서 exchange/coin/exchange_coin 적재. 시나리오용 user/round/wallet 만 별도 생성.
        ensureUserRoundWallet();
    }

    @Given("업비트에 BTC가 상장되어 있다")
    public void 업비트에_BTC가_상장되어_있다() {
        // seed-data.sql 에서 적재.
    }

    @Given("BTC 현재가는 {long}원이다")
    public void BTC_현재가는_원이다(long price) {
        livePriceAdapter.setPrice(EXCHANGE_COIN_ID, new BigDecimal(price));
    }

    @Given("지갑에 KRW 잔고가 {long}원이다")
    public void 지갑에_KRW_잔고가_원이다(long amount) {
        ensureUserRoundWallet();
        walletBalanceJpaRepository.save(
                new WalletBalanceJpaEntity(
                        WALLET_ID, KRW_COIN_ID, new BigDecimal(amount), BigDecimal.ZERO));
    }

    @Given("지갑에 BTC 잔고가 {double}개이다")
    public void 지갑에_BTC_잔고가_개이다(double amount) {
        ensureUserRoundWallet();
        walletBalanceJpaRepository.save(
                new WalletBalanceJpaEntity(
                        WALLET_ID,
                        BTC_COIN_ID,
                        new BigDecimal(String.valueOf(amount)),
                        BigDecimal.ZERO));
    }

    private void ensureUserRoundWallet() {
        Integer userCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM user WHERE user_id = ?", Integer.class, USER_ID);
        if (userCount == null || userCount == 0) {
            jdbcTemplate.update(
                    "INSERT INTO user (user_id, email, nickname, portfolio_public, created_at,"
                            + " updated_at) VALUES (?, ?, ?, true, NOW(), NOW())",
                    USER_ID,
                    "trader1@example.com",
                    "트레이더1");
        }
        Integer roundCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM investment_round WHERE round_id = ?",
                        Integer.class,
                        ROUND_ID);
        if (roundCount == null || roundCount == 0) {
            jdbcTemplate.update(
                    "INSERT INTO investment_round (round_id, version, user_id, round_number,"
                        + " initial_seed, emergency_funding_limit, emergency_charge_count, status,"
                        + " started_at) VALUES (?, 0, ?, 1, 10000000, 1000000, 0, 'ACTIVE',"
                        + " NOW())",
                    ROUND_ID,
                    USER_ID);
        }
        Integer walletCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM wallet WHERE wallet_id = ?",
                        Integer.class,
                        WALLET_ID);
        if (walletCount == null || walletCount == 0) {
            jdbcTemplate.update(
                    "INSERT INTO wallet (wallet_id, round_id, exchange_id, seed_amount, created_at)"
                            + " VALUES (?, ?, ?, 10000000, NOW())",
                    WALLET_ID,
                    ROUND_ID,
                    EXCHANGE_ID);
        }
    }

    @When("시장가 매수 주문을 {long}원으로 요청한다")
    public void 시장가_매수_주문을_원으로_요청한다(long amount) {
        Map<String, Object> body = createOrderBody("BUY", "MARKET", amount, null);
        apiClient.post("/api/orders", body);
        extractOrderIdIfSuccess();
    }

    @When("시장가 매도 주문을 {double}개로 요청한다")
    public void 시장가_매도_주문을_개로_요청한다(double amount) {
        Map<String, Object> body = createOrderBody("SELL", "MARKET", amount, null);
        apiClient.post("/api/orders", body);
        extractOrderIdIfSuccess();
    }

    @When("지정가 매수 주문을 {long}원에 가격 {long}원으로 요청한다")
    public void 지정가_매수_주문을_원에_가격_원으로_요청한다(long amount, long price) {
        Map<String, Object> body = createOrderBody("BUY", "LIMIT", amount, price);
        apiClient.post("/api/orders", body);
        extractOrderIdIfSuccess();
    }

    @When("지정가 매도 주문을 {double}개에 가격 {long}원으로 요청한다")
    public void 지정가_매도_주문을_개에_가격_원으로_요청한다(double amount, long price) {
        Map<String, Object> body = createOrderBody("SELL", "LIMIT", amount, price);
        apiClient.post("/api/orders", body);
        extractOrderIdIfSuccess();
    }

    @When("동일한 idempotencyKey로 시장가 매수 주문을 {long}원으로 {int}번 요청한다")
    public void 동일한_idempotencyKey로_시장가_매수_주문을_원으로_N번_요청한다(long amount, int count) {
        savedIdempotencyKey = UUID.randomUUID().toString();
        for (int i = 0; i < count; i++) {
            Map<String, Object> body = new HashMap<>();
            body.put("clientOrderId", savedIdempotencyKey);
            body.put("walletId", WALLET_ID);
            body.put("exchangeCoinId", EXCHANGE_COIN_ID);
            body.put("side", "BUY");
            body.put("orderType", "MARKET");
            body.put("amount", amount);
            apiClient.post("/api/orders", body);
            extractOrderIdIfSuccess();
            if (i == 0) {
                firstOrderId = lastOrderId;
            }
        }
    }

    @When("매수 주문 가능 정보를 조회한다")
    public void 매수_주문_가능_정보를_조회한다() {
        apiClient.get(
                "/api/orders/available?walletId="
                        + WALLET_ID
                        + "&exchangeCoinId="
                        + EXCHANGE_COIN_ID
                        + "&side=BUY");
    }

    @When("주문 내역을 조회한다")
    public void 주문_내역을_조회한다() {
        apiClient.get("/api/orders?walletId=" + WALLET_ID);
    }

    @When("해당 주문을 취소한다")
    public void 해당_주문을_취소한다() {
        Map<String, Object> body = Map.of("walletId", WALLET_ID);
        apiClient.post("/api/orders/" + lastOrderId + "/cancel", body);
    }

    @Then("주문 상태는 {string}이다")
    public void 주문_상태는_이다(String status) {
        apiClient.getLastResponse().expectBody().jsonPath("$.data.status").isEqualTo(status);
    }

    @Then("체결 수량은 {int}보다 크다")
    public void 체결_수량은_보다_크다(int value) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.quantity")
                .value(
                        quantity -> {
                            assertThat(new BigDecimal(quantity.toString()))
                                    .isGreaterThan(BigDecimal.ZERO);
                        });
    }

    @Then("에러 코드는 {string}이다")
    public void 에러_코드는_이다(String code) {
        apiClient.getLastResponse().expectBody().jsonPath("$.code").isEqualTo(code);
    }

    @Then("두 응답의 orderId가 동일하다")
    public void 두_응답의_orderId가_동일하다() {
        assertThat(firstOrderId).isEqualTo(lastOrderId);
    }

    @Then("주문 가능 금액은 {long}이다")
    public void 주문_가능_금액은_이다(long amount) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.available")
                .value(
                        available -> {
                            assertThat(new BigDecimal(available.toString()).longValue())
                                    .isEqualTo(amount);
                        });
    }

    @Then("주문 내역이 {int}건이다")
    public void 주문_내역이_건이다(int count) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.content.length()")
                .isEqualTo(count);
    }

    private Map<String, Object> createOrderBody(
            String side, String orderType, Number amount, Long price) {
        Map<String, Object> body = new HashMap<>();
        body.put("clientOrderId", UUID.randomUUID().toString());
        body.put("walletId", WALLET_ID);
        body.put("exchangeCoinId", EXCHANGE_COIN_ID);
        body.put("side", side);
        body.put("orderType", orderType);
        body.put("amount", amount);
        if (price != null) {
            body.put("price", price);
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private void extractOrderIdIfSuccess() {
        Map<String, Object> body =
                apiClient.getLastResponse().expectBody(Map.class).returnResult().getResponseBody();
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        if (data != null && data.get("orderId") instanceof Number num) {
            lastOrderId = num.longValue();
        }
    }
}
