package ksh.tryptobackend.acceptance.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class WithdrawalFeeStepDefinition {

    private static final Long EXCHANGE_ID = 1L;
    private static final Long COIN_ID = 2L;
    private static final BigDecimal FEE = new BigDecimal("0.00050000");
    private static final BigDecimal MIN_WITHDRAWAL = new BigDecimal("0.00100000");

    private final CommonApiClient apiClient;
    private final JdbcTemplate jdbcTemplate;

    public WithdrawalFeeStepDefinition(CommonApiClient apiClient, JdbcTemplate jdbcTemplate) {
        this.apiClient = apiClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Before("@withdrawal-fee")
    public void setUp() {
        jdbcTemplate.execute("DELETE FROM withdrawal_fee");
    }

    @Given("출금 수수료 데이터가 준비되어 있다")
    public void 출금_수수료_데이터가_준비되어_있다() {
        jdbcTemplate.update(
            "INSERT INTO withdrawal_fee (withdrawal_fee_id, exchange_id, coin_id, chain, fee, min_withdrawal) VALUES (?, ?, ?, ?, ?, ?)",
            1L, EXCHANGE_ID, COIN_ID, "ERC20", FEE, MIN_WITHDRAWAL);
    }

    @When("거래소 {long}, 코인 {long}, 체인 {string}으로 출금 수수료를 조회한다")
    public void 거래소_코인_체인으로_출금_수수료를_조회한다(Long exchangeId, Long coinId, String chain) {
        apiClient.get("/api/withdrawal-fees?exchangeId=" + exchangeId + "&coinId=" + coinId + "&chain=" + chain);
    }

    @When("거래소 {long}, 코인 {long}, 체인 {string}로 출금 수수료를 조회한다")
    public void 거래소_코인_체인로_출금_수수료를_조회한다(Long exchangeId, Long coinId, String chain) {
        apiClient.get("/api/withdrawal-fees?exchangeId=" + exchangeId + "&coinId=" + coinId + "&chain=" + chain);
    }

    @When("체인 없이 출금 수수료를 조회한다")
    public void 체인_없이_출금_수수료를_조회한다() {
        apiClient.get("/api/withdrawal-fees?exchangeId=1&coinId=2");
    }

    @Then("출금 수수료는 {bigdecimal}이다")
    public void 출금_수수료는_이다(BigDecimal fee) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.fee").value(value ->
                assertThat(new BigDecimal(value.toString()).compareTo(fee)).isZero());
    }

    @Then("최소 출금액은 {bigdecimal}이다")
    public void 최소_출금액은_이다(BigDecimal minWithdrawal) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.minWithdrawal").value(value ->
                assertThat(new BigDecimal(value.toString()).compareTo(minWithdrawal)).isZero());
    }
}
