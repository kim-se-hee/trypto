package ksh.tryptobackend.acceptance.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ksh.tryptobackend.acceptance.mock.MockEligibleRoundQueryAdapter;
import ksh.tryptobackend.ranking.adapter.out.entity.RankingJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.repository.PortfolioSnapshotJpaRepository;
import ksh.tryptobackend.ranking.adapter.out.repository.RankingJpaRepository;
import ksh.tryptobackend.ranking.adapter.out.repository.SnapshotDetailJpaRepository;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RankingBatchStepDefinition {

    private static final LocalDate SNAPSHOT_DATE = LocalDate.of(2026, 3, 1);
    private static final LocalDate COMPARISON_DATE = SNAPSHOT_DATE.minusDays(1);

    private final JobOperator jobOperator;
    private final Job rankingJob;
    private final RankingJpaRepository rankingRepository;
    private final PortfolioSnapshotJpaRepository snapshotRepository;
    private final SnapshotDetailJpaRepository detailRepository;
    private final MockEligibleRoundQueryAdapter eligibleRoundQueryAdapter;
    private final JdbcTemplate jdbcTemplate;

    private List<RankingJpaEntity> savedRankings;

    public RankingBatchStepDefinition(JobOperator jobOperator,
                                      Job rankingJob,
                                      RankingJpaRepository rankingRepository,
                                      PortfolioSnapshotJpaRepository snapshotRepository,
                                      SnapshotDetailJpaRepository detailRepository,
                                      MockEligibleRoundQueryAdapter eligibleRoundQueryAdapter,
                                      JdbcTemplate jdbcTemplate) {
        this.jobOperator = jobOperator;
        this.rankingJob = rankingJob;
        this.rankingRepository = rankingRepository;
        this.snapshotRepository = snapshotRepository;
        this.detailRepository = detailRepository;
        this.eligibleRoundQueryAdapter = eligibleRoundQueryAdapter;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Given("랭킹 배치 데이터를 초기화한다")
    public void 랭킹_배치_데이터를_초기화한다() {
        rankingRepository.deleteAllInBatch();
        detailRepository.deleteAllInBatch();
        snapshotRepository.deleteAllInBatch();
        eligibleRoundQueryAdapter.clear();
    }

    @Given("랭킹 대상 라운드가 존재한다")
    public void 랭킹_대상_라운드가_존재한다(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            Long userId = Long.valueOf(row.get("userId"));
            Long roundId = Long.valueOf(row.get("roundId"));
            int tradeCount = Integer.parseInt(row.get("tradeCount"));
            eligibleRoundQueryAdapter.addEligibleRound(
                userId, roundId, tradeCount, LocalDateTime.of(2026, 1, 1, 0, 0));
        }
    }

    @Given("스냅샷 데이터가 존재한다")
    public void 스냅샷_데이터가_존재한다(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            insertSnapshot(row, SNAPSHOT_DATE);
        }
    }

    @Given("비교 스냅샷 데이터가 존재한다")
    public void 비교_스냅샷_데이터가_존재한다(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            insertSnapshot(row, COMPARISON_DATE);
        }
    }

    @When("랭킹 배치를 실행한다")
    public void 랭킹_배치를_실행한다() throws Exception {
        JobParameters params = new JobParametersBuilder()
            .addString("snapshotDate", SNAPSHOT_DATE.toString())
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();
        jobOperator.start(rankingJob, params);
        savedRankings = rankingRepository.findAll();
    }

    @Then("랭킹 배치가 COMPLETED 상태이다")
    public void 랭킹_배치가_COMPLETED_상태이다() {
        assertThat(savedRankings).isNotNull();
    }

    @Then("DAILY 랭킹이 {int}건 생성된다")
    public void DAILY_랭킹이_건_생성된다(int count) {
        List<RankingJpaEntity> dailyRankings = savedRankings.stream()
            .filter(r -> r.getPeriod() == RankingPeriod.DAILY)
            .toList();
        assertThat(dailyRankings).hasSize(count);
    }

    @Then("{int}위의 수익률은 {double}이다")
    public void 위의_수익률은_이다(int rank, double profitRate) {
        List<RankingJpaEntity> dailyRankings = savedRankings.stream()
            .filter(r -> r.getPeriod() == RankingPeriod.DAILY)
            .sorted(Comparator.comparingInt(RankingJpaEntity::getRank))
            .toList();

        RankingJpaEntity ranking = dailyRankings.stream()
            .filter(r -> r.getRank() == rank)
            .findFirst()
            .orElseThrow();

        assertThat(ranking.getProfitRate())
            .isEqualByComparingTo(new BigDecimal(String.valueOf(profitRate)));
    }

    private void insertSnapshot(Map<String, String> row, LocalDate snapshotDate) {
        jdbcTemplate.update(
            "INSERT INTO portfolio_snapshot (user_id, round_id, exchange_id, "
                + "total_asset, total_asset_krw, total_investment, total_investment_krw, "
                + "total_profit, total_profit_rate, snapshot_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            Long.valueOf(row.get("userId")),
            Long.valueOf(row.get("roundId")),
            Long.valueOf(row.get("exchangeId")),
            new BigDecimal(row.get("totalAssetKrw")),
            new BigDecimal(row.get("totalAssetKrw")),
            new BigDecimal(row.get("totalInvestmentKrw")),
            new BigDecimal(row.get("totalInvestmentKrw")),
            new BigDecimal(row.get("totalAssetKrw")).subtract(new BigDecimal(row.get("totalInvestmentKrw"))),
            BigDecimal.ZERO,
            snapshotDate
        );
    }
}
