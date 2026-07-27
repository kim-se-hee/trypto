package ksh.tryptobackend.acceptance.steps.regretanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import org.springframework.jdbc.core.JdbcTemplate;

public class RegretReportStepDefinition {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long UNKNOWN_ROUND_ID = 9999L;
    private static final Long ROUND_ID = 301L;

    private static final Long EXCHANGE_ID_UPBIT = 1L;
    private static final Long EXCHANGE_ID_BINANCE = 3L;
    private static final Long BTC_COIN_ID = 2L;
    private static final Long ETH_COIN_ID = 3L;

    private static final Long CHASE_BUY_BAN_RULE_ID = 301L;
    private static final Long AVERAGING_DOWN_LIMIT_RULE_ID = 302L;
    private static final Long OVERTRADING_LIMIT_RULE_ID = 303L;
    private static final Long LOSS_CUT_RULE_ID = 304L;

    private static final LocalDate ANALYSIS_START = LocalDate.of(2026, 1, 15);
    private static final LocalDate ANALYSIS_END = LocalDate.of(2026, 2, 25);
    private static final LocalDateTime BINANCE_VIOLATED_AT = LocalDateTime.of(2026, 1, 22, 11, 0);
    private static final LocalDateTime UPBIT_CHASE_VIOLATED_AT = LocalDateTime.of(2026, 1, 22, 14, 30);
    private static final LocalDateTime UPBIT_ORDER_22_VIOLATED_AT = LocalDateTime.of(2026, 2, 3, 9, 45);

    private final CommonApiClient apiClient;
    private final JdbcTemplate jdbcTemplate;

    public RegretReportStepDefinition(CommonApiClient apiClient, JdbcTemplate jdbcTemplate) {
        this.apiClient = apiClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Given("복기 리포트용 기초 데이터가 준비되어 있다")
    public void prepareBaseData() {
        insertUser();
        insertRound();
        insertRules();
    }

    @Given("업비트와 바이낸스에 복기 리포트가 생성되어 있다")
    public void prepareReportsOnBothExchanges() {
        insertUpbitReport();
        insertBinanceReport();
    }

    @When("복기 리포트 조회 요청을 보낸다")
    public void getRegretReport() {
        apiClient.loginAs(USER_ID);
        apiClient.get(regretReportUrl(ROUND_ID));
    }

    @When("다른 유저로 복기 리포트 조회 요청을 보낸다")
    public void getRegretReportAsOtherUser() {
        apiClient.loginAs(OTHER_USER_ID);
        apiClient.get(regretReportUrl(ROUND_ID));
    }

    @When("없는 라운드로 복기 리포트 조회 요청을 보낸다")
    public void getRegretReportOfUnknownRound() {
        apiClient.loginAs(USER_ID);
        apiClient.get(regretReportUrl(UNKNOWN_ROUND_ID));
    }

    @Then("리포트의 위반 건수는 {int}이다")
    public void verifyTotalViolations(int count) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.totalViolations")
                .isEqualTo(count);
    }

    @Then("리포트의 위반 손실 합계는 {long}이다")
    public void verifyTotalViolationLoss(long expected) {
        verifyAmountAt("$.data.totalViolationLoss", expected);
    }

    @Then("리포트의 실제 자산은 {long}이다")
    public void verifyActualAsset(long expected) {
        verifyAmountAt("$.data.actualAsset", expected);
    }

    @Then("리포트의 원칙 준수 시 자산은 {long}이다")
    public void verifyRuleFollowedAsset(long expected) {
        verifyAmountAt("$.data.ruleFollowedAsset", expected);
    }

    @Then("규칙별 영향도는 {int}개이다")
    public void verifyRuleImpactCount(int count) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.ruleImpacts.length()")
                .isEqualTo(count);
    }

    @Then("규칙별 영향도의 위반 횟수는 모두 0이다")
    public void verifyAllRuleImpactsUnviolated() {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.ruleImpacts[?(@.violationCount != 0)]")
                .isEmpty();
    }

    @Then("{string} 원칙의 위반 횟수는 {int}이고 총 손실은 {long}이다")
    public void verifyRuleImpact(String ruleType, int violationCount, long totalLoss) {
        String path = "$.data.ruleImpacts[?(@.ruleType == '" + ruleType + "')]";
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath(path + ".violationCount")
                .isEqualTo(java.util.List.of(violationCount));
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath(path + ".totalLossAmount")
                .value(actual -> assertThat(toAmount(((java.util.List<?>) actual).getFirst()))
                        .isEqualByComparingTo(BigDecimal.valueOf(totalLoss)));
    }

    @Then("위반 상세는 {int}개이다")
    public void verifyViolationDetailCount(int count) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath("$.data.violationDetails.length()")
                .isEqualTo(count);
    }

    @Then("{int}번째 위반 상세의 거래소는 {string}이고 통화는 {string}이다")
    public void verifyViolationDetailExchange(int index, String exchangeName, String currency) {
        String path = violationDetailPath(index);
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath(path + ".exchangeName")
                .isEqualTo(exchangeName);
        apiClient.getLastResponse().expectBody().jsonPath(path + ".currency").isEqualTo(currency);
    }

    @Then("{int}번째 위반 상세의 위반 손실 합계는 {long}이고 원화 환산은 {long}이다")
    public void verifyViolationDetailLoss(int index, long totalLossAmount, long totalLossAmountKrw) {
        verifyAmountAt(violationDetailPath(index) + ".totalLossAmount", totalLossAmount);
        verifyAmountAt(violationDetailPath(index) + ".totalLossAmountKrw", totalLossAmountKrw);
    }

    @Then("{int}번째 위반 상세의 어긴 원칙은 {int}개이다")
    public void verifyViolatedRuleCount(int index, int count) {
        apiClient
                .getLastResponse()
                .expectBody()
                .jsonPath(violationDetailPath(index) + ".violatedRules.length()")
                .isEqualTo(count);
    }

    private String violationDetailPath(int index) {
        return "$.data.violationDetails[" + (index - 1) + "]";
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

    private String regretReportUrl(Long roundId) {
        return "/api/rounds/" + roundId + "/regret";
    }

    private void insertUser() {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO user (user_id, version, social_account_id, nickname, created_at,"
                        + " updated_at) VALUES (?, 0, ?, ?, ?, ?)",
                USER_ID,
                USER_ID,
                "regretReportTester",
                now,
                now);
    }

    private void insertRound() {
        jdbcTemplate.update(
                "INSERT INTO investment_round (round_id, user_id, round_number, initial_seed,"
                        + " emergency_funding_limit, emergency_charge_count, status, started_at,"
                        + " ended_at, version) VALUES (?, ?, 1, ?, ?, 0, 'ACTIVE', ?, NULL, 0)",
                ROUND_ID,
                USER_ID,
                new BigDecimal("5000000.00000000"),
                new BigDecimal("500000.00000000"),
                ANALYSIS_START.atStartOfDay());
    }

    /** LOSS_CUT 은 어긴 적이 없다. 위반 0건인 원칙도 규칙별 손실에 실려야 하는지 확인하기 위해 함께 심는다. */
    private void insertRules() {
        insertRule(CHASE_BUY_BAN_RULE_ID, "CHASE_BUY_BAN", new BigDecimal("20"));
        insertRule(AVERAGING_DOWN_LIMIT_RULE_ID, "AVERAGING_DOWN_LIMIT", new BigDecimal("2"));
        insertRule(OVERTRADING_LIMIT_RULE_ID, "OVERTRADING_LIMIT", new BigDecimal("5"));
        insertRule(LOSS_CUT_RULE_ID, "LOSS_CUT", new BigDecimal("10"));
    }

    private void insertRule(Long ruleId, String ruleType, BigDecimal thresholdValue) {
        jdbcTemplate.update(
                "INSERT INTO investment_rule (rule_id, round_id, rule_type, threshold_value,"
                        + " created_at) VALUES (?, ?, ?, ?, ?)",
                ruleId,
                ROUND_ID,
                ruleType,
                thresholdValue,
                ANALYSIS_START.atStartOfDay());
    }

    private void insertUpbitReport() {
        Long reportId = insertReport(EXCHANGE_ID_UPBIT, 3, new BigDecimal("855000"), new BigDecimal("10000000"));

        insertRuleImpact(reportId, CHASE_BUY_BAN_RULE_ID, 1, new BigDecimal("385000"));
        insertRuleImpact(reportId, AVERAGING_DOWN_LIMIT_RULE_ID, 1, new BigDecimal("120000"));
        insertRuleImpact(reportId, OVERTRADING_LIMIT_RULE_ID, 1, new BigDecimal("350000"));

        insertViolationDetail(
                reportId, 15L, CHASE_BUY_BAN_RULE_ID, BTC_COIN_ID, new BigDecimal("385000"), UPBIT_CHASE_VIOLATED_AT);
        insertViolationDetail(
                reportId,
                22L,
                AVERAGING_DOWN_LIMIT_RULE_ID,
                ETH_COIN_ID,
                new BigDecimal("120000"),
                UPBIT_ORDER_22_VIOLATED_AT);
        insertViolationDetail(
                reportId,
                22L,
                OVERTRADING_LIMIT_RULE_ID,
                ETH_COIN_ID,
                new BigDecimal("350000"),
                UPBIT_ORDER_22_VIOLATED_AT);
    }

    /** 바이낸스 금액은 USDT 다. 위반 손실이 음수인 건이라 0 으로 보정하지 않고 그대로 합산하는지도 함께 확인한다. */
    private void insertBinanceReport() {
        Long reportId = insertReport(EXCHANGE_ID_BINANCE, 1, new BigDecimal("-100"), new BigDecimal("1000"));

        insertRuleImpact(reportId, CHASE_BUY_BAN_RULE_ID, 1, new BigDecimal("-100"));
        insertViolationDetail(
                reportId, 18L, CHASE_BUY_BAN_RULE_ID, BTC_COIN_ID, new BigDecimal("-100"), BINANCE_VIOLATED_AT);
    }

    private Long insertReport(Long exchangeId, int totalViolations, BigDecimal violationLoss, BigDecimal actualAsset) {
        jdbcTemplate.update(
                "INSERT INTO regret_report (user_id, round_id, exchange_id, total_violations,"
                        + " total_violation_loss, actual_asset, rule_followed_asset, analysis_start,"
                        + " analysis_end, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                USER_ID,
                ROUND_ID,
                exchangeId,
                totalViolations,
                violationLoss,
                actualAsset,
                actualAsset.add(violationLoss),
                ANALYSIS_START,
                ANALYSIS_END,
                LocalDateTime.now());

        return jdbcTemplate.queryForObject(
                "SELECT report_id FROM regret_report WHERE round_id = ? AND exchange_id = ?",
                Long.class,
                ROUND_ID,
                exchangeId);
    }

    private void insertRuleImpact(Long reportId, Long ruleId, int violationCount, BigDecimal totalLossAmount) {
        jdbcTemplate.update(
                "INSERT INTO rule_impact (report_id, rule_id, violation_count, total_loss_amount)"
                        + " VALUES (?, ?, ?, ?)",
                reportId,
                ruleId,
                violationCount,
                totalLossAmount);
    }

    private void insertViolationDetail(
            Long reportId, Long orderId, Long ruleId, Long coinId, BigDecimal lossAmount, LocalDateTime occurredAt) {
        jdbcTemplate.update(
                "INSERT INTO violation_detail (report_id, order_id, rule_id, coin_id, loss_amount,"
                        + " occurred_at) VALUES (?, ?, ?, ?, ?, ?)",
                reportId,
                orderId,
                ruleId,
                coinId,
                lossAmount,
                occurredAt);
    }
}
