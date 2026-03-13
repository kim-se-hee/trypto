package ksh.tryptobackend.acceptance.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ksh.tryptobackend.acceptance.mock.MockBtcPriceHistoryAdapter;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import ksh.tryptobackend.regretanalysis.adapter.out.repository.RegretReportJpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RegretChartStepDefinition {

    private static final Long USER_ID = 201L;
    private static final Long ROUND_ID = 201L;
    private static final Long ROUND_ID_NO_REPORT = 202L;
    private static final Long EXCHANGE_ID_UPBIT = 1L;
    private static final Long EXCHANGE_ID_BITHUMB = 2L;

    private static final LocalDate DAY1 = LocalDate.of(2026, 1, 15);
    private static final LocalDate DAY2 = LocalDate.of(2026, 1, 16);
    private static final LocalDate DAY3 = LocalDate.of(2026, 1, 17);

    private final CommonApiClient apiClient;
    private final RegretReportJpaRepository regretReportJpaRepository;
    private final MockBtcPriceHistoryAdapter mockBtcPriceHistoryAdapter;
    private final JdbcTemplate jdbcTemplate;

    public RegretChartStepDefinition(CommonApiClient apiClient,
                                      RegretReportJpaRepository regretReportJpaRepository,
                                      MockBtcPriceHistoryAdapter mockBtcPriceHistoryAdapter,
                                      JdbcTemplate jdbcTemplate) {
        this.apiClient = apiClient;
        this.regretReportJpaRepository = regretReportJpaRepository;
        this.mockBtcPriceHistoryAdapter = mockBtcPriceHistoryAdapter;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Before
    public void setUp() {
        jdbcTemplate.execute("DELETE FROM violation_detail");
        jdbcTemplate.execute("DELETE FROM rule_impact");
        regretReportJpaRepository.deleteAllInBatch();
        jdbcTemplate.execute("DELETE FROM portfolio_snapshot WHERE round_id IN (201, 202)");
        jdbcTemplate.execute("DELETE FROM investment_round WHERE round_id IN (201, 202)");
        jdbcTemplate.update("DELETE FROM user WHERE user_id = ?", USER_ID);
        mockBtcPriceHistoryAdapter.clear();
    }

    @Given("복기 그래프 테스트 데이터가 준비되어 있다")
    public void prepareRegretChartTestData() {
        insertUser();
        insertExchanges();
        insertInvestmentRounds();
        insertRegretReports();
        insertViolationDetails();
        insertPortfolioSnapshots();
        setupMockBtcPrices();
    }

    @When("라운드 {long} 거래소 {long} 유저 {long}로 복기 그래프를 조회한다")
    public void getRegretChart(Long roundId, Long exchangeId, Long userId) {
        apiClient.get("/api/rounds/" + roundId + "/regret/chart?exchangeId=" + exchangeId + "&userId=" + userId);
    }

    @Then("복기 그래프의 거래소 이름은 {string}이다")
    public void verifyExchangeName(String exchangeName) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.exchangeName").isEqualTo(exchangeName);
    }

    @Then("복기 그래프의 기축통화는 {string}이다")
    public void verifyCurrency(String currency) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.currency").isEqualTo(currency);
    }

    @Then("자산 추이 개수는 {int}개이다")
    public void verifyAssetHistoryCount(int count) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.assetHistory.length()").isEqualTo(count);
    }

    @Then("위반 마커 개수는 {int}개이다")
    public void verifyViolationMarkerCount(int count) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.violationMarkers.length()").isEqualTo(count);
    }

    @Then("총 일수는 {int}이다")
    public void verifyTotalDays(int totalDays) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.totalDays").isEqualTo(totalDays);
    }

    private void insertUser() {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
            "INSERT INTO user (user_id, email, nickname, portfolio_public, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
            USER_ID, "regretTester@test.com", "regretTester", true, now, now);
    }

    private void insertExchanges() {
        jdbcTemplate.execute(
            "INSERT IGNORE INTO exchange_market (exchange_id, name, market_type, base_currency_coin_id) VALUES "
                + "(1, 'UPBIT', 'DOMESTIC', 1), "
                + "(2, 'BITHUMB', 'DOMESTIC', 1)");
    }

    private void insertInvestmentRounds() {
        LocalDateTime startedAt = DAY1.atStartOfDay();
        LocalDateTime endedAt = DAY3.atTime(23, 59, 59);

        jdbcTemplate.update(
            "INSERT INTO investment_round (round_id, user_id, round_number, initial_seed, "
                + "emergency_funding_limit, emergency_charge_count, status, started_at, ended_at, version) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
            ROUND_ID, USER_ID, 1L, new BigDecimal("10000000.00000000"),
            new BigDecimal("500000.00000000"), 3, "ENDED", startedAt, endedAt);

        jdbcTemplate.update(
            "INSERT INTO investment_round (round_id, user_id, round_number, initial_seed, "
                + "emergency_funding_limit, emergency_charge_count, status, started_at, ended_at, version) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
            ROUND_ID_NO_REPORT, USER_ID, 2L, new BigDecimal("10000000.00000000"),
            new BigDecimal("500000.00000000"), 3, "ENDED", startedAt, endedAt);
    }

    private void insertRegretReports() {
        LocalDateTime createdAt = LocalDateTime.now();

        jdbcTemplate.update(
            "INSERT INTO regret_report (user_id, round_id, exchange_id, total_violations, "
                + "missed_profit, actual_profit_rate, rule_followed_profit_rate, "
                + "analysis_start, analysis_end, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            USER_ID, ROUND_ID, EXCHANGE_ID_UPBIT, 2,
            new BigDecimal("150000.00000000"), new BigDecimal("4.0000"),
            new BigDecimal("5.5000"), DAY1, DAY3, createdAt);

        jdbcTemplate.update(
            "INSERT INTO regret_report (user_id, round_id, exchange_id, total_violations, "
                + "missed_profit, actual_profit_rate, rule_followed_profit_rate, "
                + "analysis_start, analysis_end, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            USER_ID, ROUND_ID, EXCHANGE_ID_BITHUMB, 0,
            new BigDecimal("0.00000000"), new BigDecimal("0.0000"),
            new BigDecimal("0.0000"), DAY1, DAY3, createdAt);
    }

    private void insertViolationDetails() {
        Long reportId = jdbcTemplate.queryForObject(
            "SELECT report_id FROM regret_report WHERE round_id = ? AND exchange_id = ?",
            Long.class, ROUND_ID, EXCHANGE_ID_UPBIT);

        jdbcTemplate.update(
            "INSERT INTO violation_detail (report_id, order_id, rule_id, coin_id, "
                + "loss_amount, profit_loss, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
            reportId, 1L, 1L, 1L,
            new BigDecimal("100000.00000000"), new BigDecimal("-100000.00000000"),
            DAY2.atTime(10, 0));

        jdbcTemplate.update(
            "INSERT INTO violation_detail (report_id, order_id, rule_id, coin_id, "
                + "loss_amount, profit_loss, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
            reportId, 2L, 2L, 2L,
            new BigDecimal("50000.00000000"), new BigDecimal("-50000.00000000"),
            DAY2.atTime(14, 0));
    }

    private void insertPortfolioSnapshots() {
        insertSnapshot(ROUND_ID, EXCHANGE_ID_UPBIT, DAY1,
            new BigDecimal("10000000.00000000"), new BigDecimal("10000000.00000000"), new BigDecimal("0.0000"));
        insertSnapshot(ROUND_ID, EXCHANGE_ID_UPBIT, DAY2,
            new BigDecimal("10200000.00000000"), new BigDecimal("10000000.00000000"), new BigDecimal("2.0000"));
        insertSnapshot(ROUND_ID, EXCHANGE_ID_UPBIT, DAY3,
            new BigDecimal("10400000.00000000"), new BigDecimal("10000000.00000000"), new BigDecimal("4.0000"));
    }

    private void insertSnapshot(Long roundId, Long exchangeId, LocalDate date,
                                 BigDecimal totalAsset, BigDecimal totalInvestment, BigDecimal profitRate) {
        jdbcTemplate.update(
            "INSERT INTO portfolio_snapshot (user_id, round_id, exchange_id, total_asset, "
                + "total_asset_krw, total_investment, total_investment_krw, total_profit, total_profit_rate, snapshot_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            USER_ID, roundId, exchangeId, totalAsset, totalAsset,
            totalInvestment, totalInvestment, totalAsset.subtract(totalInvestment), profitRate,
            date.atStartOfDay());
    }

    private void setupMockBtcPrices() {
        mockBtcPriceHistoryAdapter.setPrice(DAY1, "KRW", new BigDecimal("90000000"));
        mockBtcPriceHistoryAdapter.setPrice(DAY2, "KRW", new BigDecimal("91000000"));
        mockBtcPriceHistoryAdapter.setPrice(DAY3, "KRW", new BigDecimal("92000000"));
    }
}