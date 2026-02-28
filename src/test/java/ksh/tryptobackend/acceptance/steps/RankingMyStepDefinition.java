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

public class RankingMyStepDefinition {

    private final CommonApiClient apiClient;
    private final JdbcTemplate jdbcTemplate;

    public RankingMyStepDefinition(CommonApiClient apiClient, JdbcTemplate jdbcTemplate) {
        this.apiClient = apiClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Before
    public void setUp() {
        jdbcTemplate.execute("DELETE FROM ranking");
    }

    @Given("내 랭킹 테스트 데이터가 준비되어 있다")
    public void 내_랭킹_테스트_데이터가_준비되어_있다() {
        jdbcTemplate.execute("INSERT IGNORE INTO user (user_id, nickname, portfolio_public) VALUES (1, '테스터', true)");

        LocalDate referenceDate = LocalDate.of(2026, 3, 1);
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
            "INSERT INTO ranking (user_id, round_id, period, `rank`, profit_rate, trade_count, reference_date, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            1L, 1L, "DAILY", 1, new BigDecimal("12.5000"), 5, referenceDate, now
        );
    }

    @When("유저 {long}이 기간 {string}로 내 랭킹을 조회한다")
    public void 유저_이_기간_로_내_랭킹을_조회한다(long userId, String period) {
        apiClient.get("/api/rankings/me?userId=" + userId + "&period=" + period);
    }

    @Then("내 순위는 {int}이다")
    public void 내_순위는_이다(int rank) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.rank").isEqualTo(rank);
    }

    @Then("응답 data는 null이다")
    public void 응답_data는_null이다() {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data").isEqualTo(null);
    }
}
