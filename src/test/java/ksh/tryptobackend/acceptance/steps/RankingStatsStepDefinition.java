package ksh.tryptobackend.acceptance.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import ksh.tryptobackend.ranking.adapter.out.repository.RankingJpaRepository;
import ksh.tryptobackend.ranking.adapter.out.repository.RankingUserJpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RankingStatsStepDefinition {

    private final CommonApiClient apiClient;
    private final RankingJpaRepository rankingJpaRepository;
    private final RankingUserJpaRepository rankingUserJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    public RankingStatsStepDefinition(CommonApiClient apiClient,
                                      RankingJpaRepository rankingJpaRepository,
                                      RankingUserJpaRepository rankingUserJpaRepository,
                                      JdbcTemplate jdbcTemplate) {
        this.apiClient = apiClient;
        this.rankingJpaRepository = rankingJpaRepository;
        this.rankingUserJpaRepository = rankingUserJpaRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Before
    public void setUp() {
        rankingJpaRepository.deleteAllInBatch();
        rankingUserJpaRepository.deleteAllInBatch();
    }

    @Given("랭킹 통계 테스트 데이터가 준비되어 있다")
    public void 랭킹_통계_테스트_데이터가_준비되어_있다() {
        insertUser(1L, "user1");
        insertUser(2L, "user2");
        insertUser(3L, "user3");

        LocalDate referenceDate = LocalDate.of(2026, 3, 1);
        LocalDateTime now = LocalDateTime.now();

        insertRanking(1L, 1L, "DAILY", 1, new BigDecimal("15.5000"), 10, referenceDate, now);
        insertRanking(2L, 1L, "DAILY", 2, new BigDecimal("10.2000"), 8, referenceDate, now);
        insertRanking(3L, 1L, "DAILY", 3, new BigDecimal("5.3000"), 5, referenceDate, now);
    }

    @When("기간 {string}로 랭킹 통계를 조회한다")
    public void 기간_으로_랭킹_통계를_조회한다(String period) {
        apiClient.get("/api/rankings/stats?period=" + period);
    }

    @Then("참여자 수는 {int}이다")
    public void 참여자_수는_이다(int count) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.totalParticipants").isEqualTo(count);
    }

    @Then("최고 수익률이 존재한다")
    public void 최고_수익률이_존재한다() {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.maxProfitRate").isNotEmpty();
    }

    @Then("평균 수익률이 존재한다")
    public void 평균_수익률이_존재한다() {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.avgProfitRate").isNotEmpty();
    }

    private void insertUser(Long userId, String nickname) {
        jdbcTemplate.update(
            "INSERT INTO user (user_id, nickname, portfolio_public) VALUES (?, ?, ?)",
            userId, nickname, true
        );
    }

    private void insertRanking(Long userId, Long roundId, String period, int rank,
                                BigDecimal profitRate, int tradeCount,
                                LocalDate referenceDate, LocalDateTime createdAt) {
        jdbcTemplate.update(
            "INSERT INTO ranking (user_id, round_id, period, `rank`, profit_rate, trade_count, reference_date, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            userId, roundId, period, rank, profitRate, tradeCount, referenceDate, createdAt
        );
    }
}
