package ksh.tryptobackend.acceptance.steps.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import ksh.tryptobackend.portfolio.adapter.out.repository.PortfolioSnapshotJpaRepository;
import ksh.tryptobackend.portfolio.adapter.out.repository.SnapshotDetailJpaRepository;
import ksh.tryptobackend.ranking.adapter.out.entity.RankingJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.repository.RankingJpaRepository;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.jdbc.core.JdbcTemplate;

public class RankingBatchStepDefinition {

    private static final LocalDate SNAPSHOT_DATE = LocalDate.of(2026, 3, 1);
    private static final LocalDate COMPARISON_DATE = SNAPSHOT_DATE.minusDays(1);

    private final JobOperator jobOperator;
    private final Job rankingJob;
    private final RankingJpaRepository rankingRepository;
    private final PortfolioSnapshotJpaRepository snapshotRepository;
    private final SnapshotDetailJpaRepository detailRepository;
    private final JdbcTemplate jdbcTemplate;

    private List<RankingJpaEntity> savedRankings;

    public RankingBatchStepDefinition(
            JobOperator jobOperator,
            Job rankingJob,
            RankingJpaRepository rankingRepository,
            PortfolioSnapshotJpaRepository snapshotRepository,
            SnapshotDetailJpaRepository detailRepository,
            JdbcTemplate jdbcTemplate) {
        this.jobOperator = jobOperator;
        this.rankingJob = rankingJob;
        this.rankingRepository = rankingRepository;
        this.snapshotRepository = snapshotRepository;
        this.detailRepository = detailRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Given("랭킹 배치 데이터를 초기화한다")
    public void 랭킹_배치_데이터를_초기화한다() {
        rankingRepository.deleteAllInBatch();
        detailRepository.deleteAllInBatch();
        snapshotRepository.deleteAllInBatch();
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update(
                "DELETE FROM wallet WHERE round_id IN (SELECT round_id FROM investment_round WHERE"
                        + " status = 'ACTIVE')");
    }

    @Given("랭킹 대상 라운드가 존재한다")
    public void 랭킹_대상_라운드가_존재한다(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            Long userId = Long.valueOf(row.get("userId"));
            Long roundId = Long.valueOf(row.get("roundId"));
            int tradeCount = Integer.parseInt(row.get("tradeCount"));

            ensureActiveRound(roundId, userId);
            Long walletId = insertWalletAndGetId(roundId);
            insertFilledOrders(walletId, tradeCount);
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
        JobParameters params =
                new JobParametersBuilder()
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
        List<RankingJpaEntity> dailyRankings =
                savedRankings.stream().filter(r -> r.getPeriod() == RankingPeriod.DAILY).toList();
        assertThat(dailyRankings).hasSize(count);
    }

    @Then("{int}위의 수익률은 {double}이다")
    public void 위의_수익률은_이다(int rank, double profitRate) {
        List<RankingJpaEntity> dailyRankings =
                savedRankings.stream()
                        .filter(r -> r.getPeriod() == RankingPeriod.DAILY)
                        .sorted(Comparator.comparingInt(RankingJpaEntity::getRank))
                        .toList();

        RankingJpaEntity ranking =
                dailyRankings.stream().filter(r -> r.getRank() == rank).findFirst().orElseThrow();

        assertThat(ranking.getProfitRate())
                .isEqualByComparingTo(new BigDecimal(String.valueOf(profitRate)));
    }

    private void ensureActiveRound(Long roundId, Long userId) {
        Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM investment_round WHERE round_id = ?",
                        Integer.class,
                        roundId);
        if (count == null || count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO investment_round (round_id, version, user_id, round_number,"
                        + " initial_seed, emergency_funding_limit, emergency_charge_count, status,"
                        + " started_at) VALUES (?, 0, ?, 1, 10000000, 1000000, 0, 'ACTIVE', ?)",
                    roundId,
                    userId,
                    LocalDateTime.of(2026, 1, 1, 0, 0));
        }
    }

    private Long insertWalletAndGetId(Long roundId) {
        Long walletId = roundId * 1000;
        Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM wallet WHERE wallet_id = ?", Integer.class, walletId);
        if (count == null || count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO wallet (wallet_id, round_id, exchange_id, seed_amount, created_at)"
                            + " VALUES (?, ?, 1, 10000000, ?)",
                    walletId,
                    roundId,
                    LocalDateTime.now());
        }
        return walletId;
    }

    private void insertFilledOrders(Long walletId, int count) {
        Long userId =
                jdbcTemplate.queryForObject(
                        "SELECT user_id FROM investment_round WHERE round_id ="
                                + " (SELECT round_id FROM wallet WHERE wallet_id = ?)",
                        Long.class,
                        walletId);
        for (int i = 0; i < count; i++) {
            jdbcTemplate.update(
                    "INSERT INTO orders (idempotency_key, user_id, wallet_id, exchange_coin_id,"
                            + " coin_id, base_coin_id, exchange_name, market_symbol, order_type,"
                            + " side, order_amount, quantity, price, filled_price, fee, fee_rate,"
                            + " status, created_at, filled_at) VALUES (?, ?, ?, 10, 2, 1, 'UPBIT',"
                            + " 'BTC/KRW', 'MARKET', 'BUY', 100000, 0.001, 50000000, 50000000, 50,"
                            + " 0.0005, 'FILLED', ?, ?)",
                    java.util.UUID.randomUUID().toString(),
                    userId,
                    walletId,
                    LocalDateTime.now(),
                    LocalDateTime.now());
        }
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
                new BigDecimal(row.get("totalAssetKrw"))
                        .subtract(new BigDecimal(row.get("totalInvestmentKrw"))),
                BigDecimal.ZERO,
                snapshotDate);
    }
}
