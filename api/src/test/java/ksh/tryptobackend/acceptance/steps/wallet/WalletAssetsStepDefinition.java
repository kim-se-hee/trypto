package ksh.tryptobackend.acceptance.steps.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import org.springframework.jdbc.core.JdbcTemplate;

public class WalletAssetsStepDefinition {

    private static final Long EXCHANGE_UPBIT_ID = 1L;
    private static final Long EXCHANGE_BITHUMB_ID = 2L;
    private static final Long KRW_COIN_ID = 1L;
    private static final Long BTC_COIN_ID = 2L;
    private static final Long ETH_COIN_ID = 3L;

    private final CommonApiClient apiClient;
    private final JdbcTemplate jdbcTemplate;

    public WalletAssetsStepDefinition(CommonApiClient apiClient, JdbcTemplate jdbcTemplate) {
        this.apiClient = apiClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Given("잔고 조회 테스트 데이터가 준비되어 있다")
    public void 잔고_조회_테스트_데이터가_준비되어_있다() {
        insertUsers();
        insertCoins();
        insertExchanges();
        insertExchangeCoins();
        insertInvestmentRounds();
        insertWallets();
        insertWalletBalances();
    }

    @When("유저 {long}이 지갑 {long}의 잔고를 조회한다")
    public void 유저가_지갑의_잔고를_조회한다(Long userId, Long walletId) {
        apiClient.get("/api/users/" + userId + "/wallets/" + walletId + "/balances");
    }

    @Then("기축통화 사용 가능 잔고는 {bigdecimal}이다")
    public void 기축통화_사용_가능_잔고는_이다(BigDecimal amount) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.baseCurrencyAvailable")
                .value(
                        value ->
                                assertThat(new BigDecimal(value.toString()).compareTo(amount))
                                        .isZero());
    }

    @Then("기축통화 잠금 잔고는 {bigdecimal}이다")
    public void 기축통화_잠금_잔고는_이다(BigDecimal amount) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.baseCurrencyLocked")
                .value(
                        value ->
                                assertThat(new BigDecimal(value.toString()).compareTo(amount))
                                        .isZero());
    }

    @Then("코인 잔고 개수는 {int}개이다")
    public void 코인_잔고_개수는_개이다(int count) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.balances.length()")
                .isEqualTo(count);
    }

    @Then("첫 번째 코인의 coinId는 {long}이다")
    public void 첫_번째_코인의_coinId는_이다(Long coinId) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.balances[0].coinId")
                .isEqualTo(coinId);
    }

    @Then("첫 번째 코인의 사용 가능 잔고는 {bigdecimal}이다")
    public void 첫_번째_코인의_사용_가능_잔고는_이다(BigDecimal amount) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.balances[0].available")
                .value(
                        value ->
                                assertThat(new BigDecimal(value.toString()).compareTo(amount))
                                        .isZero());
    }

    @Then("첫 번째 코인의 잠금 잔고는 {bigdecimal}이다")
    public void 첫_번째_코인의_잠금_잔고는_이다(BigDecimal amount) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.balances[0].locked")
                .value(
                        value ->
                                assertThat(new BigDecimal(value.toString()).compareTo(amount))
                                        .isZero());
    }

    private void insertUsers() {
        jdbcTemplate.execute(
                "INSERT IGNORE INTO user (user_id, nickname, portfolio_public) VALUES "
                        + "(1, '트레이더1', true), "
                        + "(2, '트레이더2', true)");
    }

    private void insertCoins() {
        jdbcTemplate.execute(
                "INSERT IGNORE INTO coin (coin_id, symbol, name) VALUES "
                        + "(1, 'KRW', '원화'), "
                        + "(2, 'BTC', '비트코인'), "
                        + "(3, 'ETH', '이더리움')");
    }

    private void insertExchanges() {
        jdbcTemplate.execute(
                "INSERT IGNORE INTO exchange_market (exchange_id, name, market_type,"
                        + " base_currency_coin_id, fee_rate) VALUES (1, 'Upbit', 'DOMESTIC', 1,"
                        + " 0.000500), (2, 'Bithumb', 'DOMESTIC', 1, 0.000500)");
    }

    private void insertExchangeCoins() {
        jdbcTemplate.execute(
                "INSERT IGNORE INTO exchange_coin (exchange_coin_id, exchange_id, coin_id) VALUES "
                        + "(10, 1, 2), "
                        + "(11, 1, 3)");
    }

    private void insertInvestmentRounds() {
        LocalDateTime now = LocalDateTime.now();
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
        jdbcTemplate.update(
                "INSERT INTO wallet (wallet_id, round_id, exchange_id, seed_amount, created_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                1L,
                1L,
                EXCHANGE_UPBIT_ID,
                new BigDecimal("10000000.00000000"),
                now);
        jdbcTemplate.update(
                "INSERT INTO wallet (wallet_id, round_id, exchange_id, seed_amount, created_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                2L,
                1L,
                EXCHANGE_BITHUMB_ID,
                new BigDecimal("5000000.00000000"),
                now);
    }

    private void insertWalletBalances() {
        // 지갑 1: KRW(기축통화) + BTC + ETH
        jdbcTemplate.update(
                "INSERT INTO wallet_balance (wallet_id, coin_id, available, locked) VALUES (?, ?,"
                        + " ?, ?)",
                1L,
                KRW_COIN_ID,
                new BigDecimal("2450000.00000000"),
                new BigDecimal("150000.00000000"));
        jdbcTemplate.update(
                "INSERT INTO wallet_balance (wallet_id, coin_id, available, locked) VALUES (?, ?,"
                        + " ?, ?)",
                1L,
                BTC_COIN_ID,
                new BigDecimal("0.05234100"),
                new BigDecimal("0.00100000"));
        jdbcTemplate.update(
                "INSERT INTO wallet_balance (wallet_id, coin_id, available, locked) VALUES (?, ?,"
                        + " ?, ?)",
                1L,
                ETH_COIN_ID,
                new BigDecimal("1.24500000"),
                BigDecimal.ZERO);
        // 지갑 2: KRW(기축통화)만 존재
        jdbcTemplate.update(
                "INSERT INTO wallet_balance (wallet_id, coin_id, available, locked) VALUES (?, ?,"
                        + " ?, ?)",
                2L,
                KRW_COIN_ID,
                new BigDecimal("5000000.00000000"),
                BigDecimal.ZERO);
    }
}
