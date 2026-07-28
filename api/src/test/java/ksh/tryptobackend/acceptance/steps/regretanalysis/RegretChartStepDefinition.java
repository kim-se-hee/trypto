package ksh.tryptobackend.acceptance.steps.regretanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import ksh.tryptobackend.acceptance.mock.MockBtcPriceHistoryAdapter;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import org.springframework.jdbc.core.JdbcTemplate;

public class RegretChartStepDefinition {

    private static final Long USER_ID = 201L;
    private static final Long ROUND_ID = 201L;
    private static final Long ROUND_ID_NO_REPORT = 202L;
    private static final Long ROUND_ID_NO_SNAPSHOT = 203L;
    private static final Long EXCHANGE_ID_UPBIT = 1L;
    private static final Long EXCHANGE_ID_BINANCE = 3L;
    private static final Long BTC_COIN_ID = 2L;
    private static final Long CHASE_BUY_BAN_RULE_ID = 201L;

    private static final BigDecimal INITIAL_SEED = new BigDecimal("10000000.00000000");
    private static final BigDecimal USDT_TO_KRW = new BigDecimal("1400");

    private static final LocalDate DAY1 = LocalDate.of(2026, 1, 15);
    private static final LocalDate DAY2 = LocalDate.of(2026, 1, 16);
    private static final LocalDate DAY3 = LocalDate.of(2026, 1, 17);

    private final CommonApiClient apiClient;
    private final MockBtcPriceHistoryAdapter mockBtcPriceHistoryAdapter;
    private final JdbcTemplate jdbcTemplate;

    public RegretChartStepDefinition(
            CommonApiClient apiClient,
            MockBtcPriceHistoryAdapter mockBtcPriceHistoryAdapter,
            JdbcTemplate jdbcTemplate) {
        this.apiClient = apiClient;
        this.mockBtcPriceHistoryAdapter = mockBtcPriceHistoryAdapter;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Given("복기 그래프 테스트 데이터가 준비되어 있다")
    public void prepareRegretChartTestData() {
        insertUser();
        insertInvestmentRounds();
        insertRule();
        insertEmergencyFunding();
        insertRegretReports();
        insertPortfolioSnapshots();
        setupMockBtcPrices();
    }

    @When("라운드 {long} 유저 {long}로 복기 그래프를 조회한다")
    public void getRegretChart(Long roundId, Long userId) {
        apiClient.loginAs(userId);
        apiClient.get("/api/rounds/" + roundId + "/regret/chart");
    }

    @Then("자산 추이 개수는 {int}개이다")
    public void verifyAssetHistoryCount(int count) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.assetHistory.length()")
                .isEqualTo(count);
    }

    @Then("위반 마커 개수는 {int}개이다")
    public void verifyViolationMarkerCount(int count) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.violationMarkers.length()")
                .isEqualTo(count);
    }

    @Then("총 일수는 {int}이다")
    public void verifyTotalDays(int totalDays) {
        apiClient.getLastResponse().expectBody().jsonPath("$.data.totalDays").isEqualTo(totalDays);
    }

    @Then("{int}일차 실제 자산은 {long}이다")
    public void verifyActualAsset(int day, long expected) {
        verifyAmountAt("$.data.assetHistory[" + (day - 1) + "].actualAsset", expected);
    }

    @Then("{int}일차 원칙 준수 시 자산은 {long}이다")
    public void verifyRuleFollowedAsset(int day, long expected) {
        verifyAmountAt("$.data.assetHistory[" + (day - 1) + "].ruleFollowedAsset", expected);
    }

    @Then("{int}일차 BTC 홀드 자산은 {long}이다")
    public void verifyBtcHoldAsset(int day, long expected) {
        verifyAmountAt("$.data.assetHistory[" + (day - 1) + "].btcHoldAsset", expected);
    }

    @Then("{int}번째 위반 마커의 날짜는 {string}이고 자산은 {long}이다")
    public void verifyViolationMarker(int index, String date, long assetValue) {
        String path = "$.data.violationMarkers[" + (index - 1) + "]";
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath(path + ".snapshotDate")
                .isEqualTo(date);
        verifyAmountAt(path + ".assetValue", assetValue);
    }

    @Then("모든 날짜의 원칙 준수 시 자산은 실제 자산과 같다")
    public void verifyRuleFollowedAssetEqualsActual() {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.assetHistory")
                .value(history -> assertThat((List<?>) history)
                        .allSatisfy(entry -> assertThat(toAmount(((java.util.Map<?, ?>) entry).get("actualAsset")))
                                .isEqualByComparingTo(
                                        toAmount(((java.util.Map<?, ?>) entry).get("ruleFollowedAsset")))));
    }

    private void verifyAmountAt(String jsonPath, long expected) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath(jsonPath)
                .value(actual -> assertThat(toAmount(actual)).isEqualByComparingTo(BigDecimal.valueOf(expected)));
    }

    private static BigDecimal toAmount(Object value) {
        return new BigDecimal(value.toString());
    }

    private void insertUser() {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO user (user_id, version, social_account_id, nickname,"
                        + " created_at, updated_at) VALUES (?, 0, ?, ?, ?, ?)",
                USER_ID,
                USER_ID,
                "regretTester",
                now,
                now);
    }

    private void insertInvestmentRounds() {
        insertRound(ROUND_ID, 1L);
        insertRound(ROUND_ID_NO_REPORT, 2L);
        insertRound(ROUND_ID_NO_SNAPSHOT, 3L);
    }

    private void insertRound(Long roundId, Long roundNumber) {
        jdbcTemplate.update(
                "INSERT INTO investment_round (round_id, user_id, round_number, initial_seed,"
                        + " emergency_funding_limit, emergency_charge_count, status, started_at,"
                        + " ended_at, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                roundId,
                USER_ID,
                roundNumber,
                INITIAL_SEED,
                new BigDecimal("500000.00000000"),
                3,
                "ENDED",
                DAY1.atStartOfDay(),
                DAY3.atTime(23, 59, 59));
    }

    private void insertRule() {
        jdbcTemplate.update(
                "INSERT INTO investment_rule (rule_id, round_id, rule_type, threshold_value,"
                        + " created_at) VALUES (?, ?, ?, ?, ?)",
                CHASE_BUY_BAN_RULE_ID,
                ROUND_ID,
                "CHASE_BUY_BAN",
                new BigDecimal("20"),
                DAY1.atStartOfDay());
    }

    /** BTC 홀드 벤치마크는 긴급 충전 금액을 충전일 종가로 추가 매수한 것으로 본다. 원화 충전이라 환산액은 투입 금액과 같다. */
    private void insertEmergencyFunding() {
        jdbcTemplate.update(
                "INSERT INTO emergency_funding (round_id, exchange_id, amount, krw_converted_amount, created_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                ROUND_ID,
                EXCHANGE_ID_UPBIT,
                new BigDecimal("400000.00000000"),
                new BigDecimal("400000.00000000"),
                DAY2.atTime(9, 0));
    }

    private void insertRegretReports() {
        insertReport(ROUND_ID, EXCHANGE_ID_UPBIT, 1, new BigDecimal("100000"), new BigDecimal("8500000"));
        insertReport(ROUND_ID, EXCHANGE_ID_BINANCE, 1, new BigDecimal("50"), new BigDecimal("1000"));

        insertViolationDetail(ROUND_ID, EXCHANGE_ID_UPBIT, new BigDecimal("100000"), DAY2.atTime(10, 0));
        insertViolationDetail(ROUND_ID, EXCHANGE_ID_BINANCE, new BigDecimal("50"), DAY2.atTime(14, 0));
    }

    private void insertReport(
            Long roundId, Long exchangeId, int totalViolations, BigDecimal violationLoss, BigDecimal actualAsset) {
        jdbcTemplate.update(
                "INSERT INTO regret_report (user_id, round_id, exchange_id, total_violations,"
                        + " total_violation_loss, actual_asset, analysis_start, analysis_end,"
                        + " created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                USER_ID,
                roundId,
                exchangeId,
                totalViolations,
                violationLoss,
                actualAsset,
                DAY1,
                DAY3,
                LocalDateTime.now());
    }

    private void insertViolationDetail(Long roundId, Long exchangeId, BigDecimal lossAmount, LocalDateTime occurredAt) {
        Long reportId = jdbcTemplate.queryForObject(
                "SELECT report_id FROM regret_report WHERE round_id = ? AND exchange_id = ?",
                Long.class,
                roundId,
                exchangeId);

        jdbcTemplate.update(
                "INSERT INTO violation_detail (report_id, order_id, rule_id, coin_id, loss_amount,"
                        + " occurred_at) VALUES (?, ?, ?, ?, ?, ?)",
                reportId,
                1L,
                CHASE_BUY_BAN_RULE_ID,
                BTC_COIN_ID,
                lossAmount,
                occurredAt);
    }

    /** 거래소별 자산을 원화로 합쳐야 그날의 실제 자산이 된다. 바이낸스 몫은 1 USDT = 1,400원으로 환산해 적재한다. */
    private void insertPortfolioSnapshots() {
        insertSnapshot(ROUND_ID, EXCHANGE_ID_UPBIT, DAY1, new BigDecimal("8600000"), new BigDecimal("8600000"));
        insertSnapshot(ROUND_ID, EXCHANGE_ID_UPBIT, DAY2, new BigDecimal("8400000"), new BigDecimal("8400000"));
        insertSnapshot(ROUND_ID, EXCHANGE_ID_UPBIT, DAY3, new BigDecimal("8500000"), new BigDecimal("8500000"));

        insertBinanceSnapshot(ROUND_ID, DAY1, new BigDecimal("1000"));
        insertBinanceSnapshot(ROUND_ID, DAY2, new BigDecimal("1000"));
        insertBinanceSnapshot(ROUND_ID, DAY3, new BigDecimal("1000"));

        insertSnapshot(
                ROUND_ID_NO_REPORT, EXCHANGE_ID_UPBIT, DAY1, new BigDecimal("10000000"), new BigDecimal("10000000"));
        insertSnapshot(
                ROUND_ID_NO_REPORT, EXCHANGE_ID_UPBIT, DAY2, new BigDecimal("10200000"), new BigDecimal("10200000"));
    }

    private void insertBinanceSnapshot(Long roundId, LocalDate date, BigDecimal totalAsset) {
        insertSnapshot(roundId, EXCHANGE_ID_BINANCE, date, totalAsset, totalAsset.multiply(USDT_TO_KRW));
    }

    private void insertSnapshot(
            Long roundId, Long exchangeId, LocalDate date, BigDecimal totalAsset, BigDecimal totalAssetKrw) {
        jdbcTemplate.update(
                "INSERT INTO portfolio_snapshot (user_id, round_id, exchange_id, total_asset,"
                        + " total_asset_krw, snapshot_date) VALUES (?, ?, ?, ?, ?, ?)",
                USER_ID,
                roundId,
                exchangeId,
                totalAsset,
                totalAssetKrw,
                date);
    }

    private void setupMockBtcPrices() {
        mockBtcPriceHistoryAdapter.setPrice(DAY1, "KRW", new BigDecimal("50000000"));
        mockBtcPriceHistoryAdapter.setPrice(DAY2, "KRW", new BigDecimal("40000000"));
        mockBtcPriceHistoryAdapter.setPrice(DAY3, "KRW", new BigDecimal("50000000"));
    }
}
