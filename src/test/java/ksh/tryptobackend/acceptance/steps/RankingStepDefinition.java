package ksh.tryptobackend.acceptance.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import ksh.tryptobackend.ranking.adapter.out.repository.RankingJpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RankingStepDefinition {

    private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 3, 1);
    private static final Long ROUND_ID = 1L;

    private final CommonApiClient apiClient;
    private final RankingJpaRepository rankingJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    public RankingStepDefinition(CommonApiClient apiClient,
                                 RankingJpaRepository rankingJpaRepository,
                                 JdbcTemplate jdbcTemplate) {
        this.apiClient = apiClient;
        this.rankingJpaRepository = rankingJpaRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Before
    public void setUp() {
        rankingJpaRepository.deleteAllInBatch();
        jdbcTemplate.update("DELETE FROM user WHERE user_id IN (101, 102, 103)");
    }

    @Given("랭킹 테스트 데이터가 준비되어 있다")
    public void 랭킹_테스트_데이터가_준비되어_있다() {
        insertUser(101L, "유저A", true);
        insertUser(102L, "유저B", false);
        insertUser(103L, "유저C", true);

        insertRanking(101L, 1, new BigDecimal("15.5000"), 10);
        insertRanking(102L, 2, new BigDecimal("10.2500"), 7);
        insertRanking(103L, 3, new BigDecimal("5.1000"), 3);
    }

    @When("기간 {string}로 랭킹 목록을 조회한다")
    public void 기간으로_랭킹_목록을_조회한다(String period) {
        apiClient.get("/api/rankings?period=" + period);
    }

    @When("기간 {string} 기준 날짜 {string}로 랭킹 목록을 조회한다")
    public void 기간_기준_날짜로_랭킹_목록을_조회한다(String period, String referenceDate) {
        apiClient.get("/api/rankings?period=" + period + "&referenceDate=" + referenceDate);
    }

    @When("기간 {string} 크기 {int}로 랭킹 목록을 조회한다")
    public void 기간_크기로_랭킹_목록을_조회한다(String period, int size) {
        apiClient.get("/api/rankings?period=" + period + "&size=" + size);
    }

    @When("기간 {string} 커서 {int} 크기 {int}로 랭킹 목록을 조회한다")
    public void 기간_커서_크기로_랭킹_목록을_조회한다(String period, int cursorRank, int size) {
        apiClient.get("/api/rankings?period=" + period + "&cursorRank=" + cursorRank + "&size=" + size);
    }

    @Then("랭킹 목록 개수는 {int}개이다")
    public void 랭킹_목록_개수는_개이다(int count) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.content.length()").isEqualTo(count);
    }

    @Then("첫 번째 랭킹의 순위는 {int}이다")
    public void 첫_번째_랭킹의_순위는_이다(int rank) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.content[0].rank").isEqualTo(rank);
    }

    @Then("다음 페이지가 존재한다")
    public void 다음_페이지가_존재한다() {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.hasNext").isEqualTo(true);
    }

    @Then("다음 페이지가 존재하지 않는다")
    public void 다음_페이지가_존재하지_않는다() {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.hasNext").isEqualTo(false);
    }

    private void insertUser(Long userId, String nickname, boolean portfolioPublic) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
            "INSERT INTO user (user_id, email, nickname, portfolio_public, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
            userId, nickname + "@test.com", nickname, portfolioPublic, now, now
        );
    }

    private void insertRanking(Long userId, int rank, BigDecimal profitRate, int tradeCount) {
        jdbcTemplate.update(
            "INSERT INTO ranking (user_id, round_id, period, `rank`, profit_rate, trade_count, reference_date, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            userId, ROUND_ID, "DAILY", rank, profitRate, tradeCount, REFERENCE_DATE, LocalDateTime.now()
        );
    }
}
