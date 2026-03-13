package ksh.tryptobackend.acceptance.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RankerPortfolioStepDefinition {

    private final CommonApiClient apiClient;
    private final JdbcTemplate jdbcTemplate;

    public RankerPortfolioStepDefinition(CommonApiClient apiClient, JdbcTemplate jdbcTemplate) {
        this.apiClient = apiClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Before
    public void setUp() {
        jdbcTemplate.execute("DELETE FROM portfolio_snapshot_detail");
        jdbcTemplate.execute("DELETE FROM portfolio_snapshot");
        jdbcTemplate.execute("DELETE FROM ranking");
    }

    @Given("포트폴리오 테스트 데이터가 준비되어 있다")
    public void 포트폴리오_테스트_데이터가_준비되어_있다() {
        insertUsers();
        insertExchangeAndCoins();
        insertRankings();
        insertInvestmentRound();
        insertPortfolioSnapshot();
    }

    @When("유저 {long}의 포트폴리오를 기간 {string}로 조회한다")
    public void 유저의_포트폴리오를_기간으로_조회한다(Long userId, String period) {
        apiClient.get("/api/rankings/" + userId + "/portfolio?period=" + period);
    }

    @Then("포트폴리오 순위는 {int}이다")
    public void 포트폴리오_순위는_이다(int rank) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.rank").isEqualTo(rank);
    }

    @Then("보유 자산 개수는 {int}개이다")
    public void 보유_자산_개수는_개이다(int count) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.holdings.length()").isEqualTo(count);
    }

    private void insertUsers() {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
            "INSERT IGNORE INTO user (user_id, email, nickname, portfolio_public, created_at, updated_at) VALUES "
                + "(1, 'trader1@test.com', '트레이더1', true, ?, ?), "
                + "(2, 'trader2@test.com', '트레이더2', false, ?, ?), "
                + "(3, 'trader3@test.com', '트레이더3', true, ?, ?)",
            now, now, now, now, now, now);
    }

    private void insertExchangeAndCoins() {
        jdbcTemplate.execute("INSERT IGNORE INTO exchange_market (exchange_id, name, market_type, base_currency_coin_id) VALUES "
            + "(1, 'UPBIT', 'DOMESTIC', 1)");
        jdbcTemplate.execute("INSERT IGNORE INTO coin (coin_id, symbol, name) VALUES "
            + "(1, 'BTC', 'Bitcoin'), (2, 'ETH', 'Ethereum')");
    }

    private void insertRankings() {
        LocalDate referenceDate = LocalDate.of(2026, 3, 1);
        LocalDateTime createdAt = referenceDate.atStartOfDay();
        jdbcTemplate.update(
            "INSERT INTO ranking (user_id, round_id, period, `rank`, profit_rate, trade_count, reference_date, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            1L, 1L, "DAILY", 1, new BigDecimal("15.5000"), 10, referenceDate, createdAt);
        jdbcTemplate.update(
            "INSERT INTO ranking (user_id, round_id, period, `rank`, profit_rate, trade_count, reference_date, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            2L, 2L, "DAILY", 2, new BigDecimal("12.3000"), 8, referenceDate, createdAt);
        jdbcTemplate.update(
            "INSERT INTO ranking (user_id, round_id, period, `rank`, profit_rate, trade_count, reference_date, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            3L, 3L, "DAILY", 101, new BigDecimal("5.1000"), 5, referenceDate, createdAt);
    }

    private void insertInvestmentRound() {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.execute("DELETE FROM investment_round WHERE user_id = 1");
        jdbcTemplate.update(
            "INSERT INTO investment_round (round_id, user_id, round_number, initial_seed, emergency_funding_limit, emergency_charge_count, status, started_at, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            1L, 1L, 1L, new BigDecimal("10000000.00000000"), new BigDecimal("500000.00000000"), 3, "ACTIVE", now, 0L);
    }

    private void insertPortfolioSnapshot() {
        LocalDateTime snapshotDate = LocalDateTime.of(2026, 3, 1, 0, 0);
        jdbcTemplate.update(
            "INSERT INTO portfolio_snapshot (snapshot_id, user_id, round_id, exchange_id, total_asset, total_asset_krw, total_investment, total_investment_krw, total_profit, total_profit_rate, snapshot_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            1L, 1L, 1L, 1L,
            new BigDecimal("11550000.00000000"), new BigDecimal("11550000.00000000"),
            new BigDecimal("10000000.00000000"), new BigDecimal("10000000.00000000"),
            new BigDecimal("1550000.00000000"),
            new BigDecimal("15.5000"), snapshotDate);

        jdbcTemplate.update(
            "INSERT INTO portfolio_snapshot_detail (detail_id, snapshot_id, coin_id, quantity, avg_buy_price, current_price, profit_rate, asset_ratio) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            1L, 1L, 1L,
            new BigDecimal("0.10000000"), new BigDecimal("90000000.00000000"), new BigDecimal("95000000.00000000"),
            new BigDecimal("5.5600"), new BigDecimal("60.0000"));

        jdbcTemplate.update(
            "INSERT INTO portfolio_snapshot_detail (detail_id, snapshot_id, coin_id, quantity, avg_buy_price, current_price, profit_rate, asset_ratio) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            2L, 1L, 2L,
            new BigDecimal("1.00000000"), new BigDecimal("4000000.00000000"), new BigDecimal("4600000.00000000"),
            new BigDecimal("15.0000"), new BigDecimal("40.0000"));
    }
}
