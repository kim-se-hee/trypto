package ksh.tryptobackend.acceptance.steps.portfolio;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.acceptance.mock.MockHoldingAdapter;
import ksh.tryptobackend.acceptance.mock.MockLivePriceAdapter;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import org.springframework.jdbc.core.JdbcTemplate;

public class MyHoldingsStepDefinition {

    private static final Long EXCHANGE_ID = 1L;
    private static final Long KRW_COIN_ID = 1L;
    private static final Long BTC_COIN_ID = 2L;
    private static final Long ETH_COIN_ID = 3L;
    private static final Long BTC_EXCHANGE_COIN_ID = 10L;
    private static final Long ETH_EXCHANGE_COIN_ID = 11L;

    private final CommonApiClient apiClient;
    private final JdbcTemplate jdbcTemplate;
    private final MockHoldingAdapter holdingAdapter;
    private final MockLivePriceAdapter livePriceAdapter;

    public MyHoldingsStepDefinition(
            CommonApiClient apiClient,
            JdbcTemplate jdbcTemplate,
            MockHoldingAdapter holdingAdapter,
            MockLivePriceAdapter livePriceAdapter) {
        this.apiClient = apiClient;
        this.jdbcTemplate = jdbcTemplate;
        this.holdingAdapter = holdingAdapter;
        this.livePriceAdapter = livePriceAdapter;
    }

    @Given("보유 현황 테스트 데이터가 준비되어 있다")
    public void 보유_현황_테스트_데이터가_준비되어_있다() {
        insertUsers();
        insertExchangeAndCoins();
        insertExchangeCoinMappings();
        insertInvestmentRound();
        insertWallets();
        insertWalletBalances();
        setUpHoldings();
        setUpLivePrices();
    }

    @When("유저 {long}이 지갑 {long}의 포트폴리오를 조회한다")
    public void 유저가_지갑의_포트폴리오를_조회한다(Long userId, Long walletId) {
        apiClient.get("/api/users/" + userId + "/wallets/" + walletId + "/portfolio");
    }

    @Then("거래소 ID는 {long}이다")
    public void 거래소_ID는_이다(Long exchangeId) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.exchangeId")
                .isEqualTo(exchangeId);
    }

    @Then("기축통화 심볼은 {string}이다")
    public void 기축통화_심볼은_이다(String symbol) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.baseCurrencySymbol")
                .isEqualTo(symbol);
    }

    @Then("기축통화 잔고는 {long}이다")
    public void 기축통화_잔고는_이다(Long balance) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.baseCurrencyBalance")
                .isEqualTo(balance);
    }

    @Then("보유 코인 개수는 {int}개이다")
    public void 보유_코인_개수는_개이다(int count) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.holdings.length()")
                .isEqualTo(count);
    }

    @Then("첫 번째 코인 심볼은 {string}이다")
    public void 첫_번째_코인_심볼은_이다(String symbol) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.holdings[0].coinSymbol")
                .isEqualTo(symbol);
    }

    @Then("첫 번째 코인 현재가는 {long}이다")
    public void 첫_번째_코인_현재가는_이다(Long price) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.holdings[0].currentPrice")
                .isEqualTo(price);
    }

    private void insertUsers() {
        jdbcTemplate.execute(
                "INSERT IGNORE INTO user (user_id, nickname, portfolio_public) VALUES "
                        + "(1, '트레이더1', true), "
                        + "(2, '트레이더2', true)");
    }

    private void insertExchangeAndCoins() {
        jdbcTemplate.execute(
                "INSERT IGNORE INTO exchange_market (exchange_id, name, market_type,"
                        + " base_currency_coin_id, fee_rate) VALUES (1, 'Upbit', 'DOMESTIC', 1,"
                        + " 0.000500), (2, 'Bithumb', 'DOMESTIC', 1, 0.000500)");
        jdbcTemplate.execute(
                "INSERT IGNORE INTO coin (coin_id, symbol, name) VALUES "
                        + "(1, 'KRW', '원화'), "
                        + "(2, 'BTC', '비트코인'), "
                        + "(3, 'ETH', '이더리움')");
    }

    private void insertExchangeCoinMappings() {
        jdbcTemplate.execute(
                "INSERT IGNORE INTO exchange_coin (exchange_coin_id, exchange_id, coin_id) VALUES "
                        + "(10, 1, 2), "
                        + "(11, 1, 3)");
    }

    private void insertInvestmentRound() {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.execute("DELETE FROM investment_round WHERE round_id IN (1, 2)");
        jdbcTemplate.update(
                "INSERT INTO investment_round (round_id, user_id, round_number, initial_seed,"
                        + " emergency_funding_limit, emergency_charge_count, status, started_at,"
                        + " version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                1L,
                1L,
                1L,
                new BigDecimal("10000000.00000000"),
                new BigDecimal("500000.00000000"),
                3,
                "ACTIVE",
                now,
                0L);
        jdbcTemplate.update(
                "INSERT INTO investment_round (round_id, user_id, round_number, initial_seed,"
                        + " emergency_funding_limit, emergency_charge_count, status, started_at,"
                        + " version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                2L,
                2L,
                1L,
                new BigDecimal("10000000.00000000"),
                new BigDecimal("500000.00000000"),
                3,
                "ACTIVE",
                now,
                0L);
    }

    private void insertWallets() {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.execute("DELETE FROM wallet WHERE wallet_id IN (1, 2)");
        jdbcTemplate.update(
                "INSERT INTO wallet (wallet_id, round_id, exchange_id, seed_amount, created_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                1L,
                1L,
                EXCHANGE_ID,
                new BigDecimal("10000000.00000000"),
                now);
        jdbcTemplate.update(
                "INSERT INTO wallet (wallet_id, round_id, exchange_id, seed_amount, created_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                2L,
                1L,
                2L,
                new BigDecimal("5000000.00000000"),
                now);
    }

    private void insertWalletBalances() {
        jdbcTemplate.execute("DELETE FROM wallet_balance WHERE wallet_id IN (1, 2)");
        jdbcTemplate.update(
                "INSERT INTO wallet_balance (wallet_id, coin_id, available, locked) VALUES (?, ?,"
                        + " ?, ?)",
                1L,
                KRW_COIN_ID,
                new BigDecimal("2450000.00000000"),
                BigDecimal.ZERO);
        jdbcTemplate.update(
                "INSERT INTO wallet_balance (wallet_id, coin_id, available, locked) VALUES (?, ?,"
                        + " ?, ?)",
                2L,
                KRW_COIN_ID,
                new BigDecimal("5000000.00000000"),
                BigDecimal.ZERO);
    }

    private void setUpHoldings() {
        holdingAdapter.setHolding(
                1L, BTC_COIN_ID, new BigDecimal("132500000"), new BigDecimal("0.052341"), 0);
        holdingAdapter.setHolding(
                1L, ETH_COIN_ID, new BigDecimal("5120000"), new BigDecimal("1.245"), 0);
    }

    private void setUpLivePrices() {
        livePriceAdapter.setPrice(BTC_EXCHANGE_COIN_ID, new BigDecimal("143250000"));
        livePriceAdapter.setPrice(ETH_EXCHANGE_COIN_ID, new BigDecimal("4821000"));
    }
}
