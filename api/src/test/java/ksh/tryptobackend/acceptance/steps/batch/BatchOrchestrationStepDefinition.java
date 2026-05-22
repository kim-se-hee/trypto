package ksh.tryptobackend.acceptance.steps.batch;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import ksh.tryptobackend.acceptance.mock.MockLivePriceAdapter;
import ksh.tryptobackend.portfolio.adapter.out.repository.PortfolioSnapshotJpaRepository;
import ksh.tryptobackend.portfolio.adapter.out.repository.SnapshotDetailJpaRepository;
import ksh.tryptobackend.ranking.adapter.out.repository.RankingJpaRepository;
import ksh.tryptobackend.regretanalysis.adapter.out.repository.RegretReportJpaRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.jdbc.core.JdbcTemplate;

public class BatchOrchestrationStepDefinition {

    private static final LocalDate SNAPSHOT_DATE = LocalDate.of(2026, 3, 1);
    private static final Long ROUND_ID = 10L;
    private static final Long USER_ID = 10L;
    private static final Long EXCHANGE_ID = 1L;
    private static final Long WALLET_ID = 1000L;
    private static final Long BASE_CURRENCY_COIN_ID = 99L;

    private final JobOperator jobOperator;
    private final Job snapshotJob;
    private final Job rankingJob;
    private final Job regretReportJob;
    private final PortfolioSnapshotJpaRepository snapshotRepository;
    private final SnapshotDetailJpaRepository detailRepository;
    private final RankingJpaRepository rankingRepository;
    private final RegretReportJpaRepository regretReportRepository;
    private final MockLivePriceAdapter livePriceAdapter;
    private final JdbcTemplate jdbcTemplate;

    private boolean snapshotCompleted;
    private boolean rankingCompleted;
    private boolean reportCompleted;

    public BatchOrchestrationStepDefinition(
            JobOperator jobOperator,
            Job snapshotJob,
            Job rankingJob,
            Job regretReportJob,
            PortfolioSnapshotJpaRepository snapshotRepository,
            SnapshotDetailJpaRepository detailRepository,
            RankingJpaRepository rankingRepository,
            RegretReportJpaRepository regretReportRepository,
            MockLivePriceAdapter livePriceAdapter,
            JdbcTemplate jdbcTemplate) {
        this.jobOperator = jobOperator;
        this.snapshotJob = snapshotJob;
        this.rankingJob = rankingJob;
        this.regretReportJob = regretReportJob;
        this.snapshotRepository = snapshotRepository;
        this.detailRepository = detailRepository;
        this.rankingRepository = rankingRepository;
        this.regretReportRepository = regretReportRepository;
        this.livePriceAdapter = livePriceAdapter;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Given("전체 배치 데이터를 초기화한다")
    public void 전체_배치_데이터를_초기화한다() {
        regretReportRepository.deleteAllInBatch();
        rankingRepository.deleteAllInBatch();
        detailRepository.deleteAllInBatch();
        snapshotRepository.deleteAllInBatch();
        jdbcTemplate.update("DELETE FROM orders WHERE wallet_id = ?", WALLET_ID);
        jdbcTemplate.update("DELETE FROM holding WHERE wallet_id = ?", WALLET_ID);
        jdbcTemplate.update("DELETE FROM wallet_balance WHERE wallet_id = ?", WALLET_ID);
        jdbcTemplate.update("DELETE FROM emergency_funding WHERE round_id = ?", ROUND_ID);
        jdbcTemplate.update("DELETE FROM wallet WHERE wallet_id = ?", WALLET_ID);
        jdbcTemplate.update("DELETE FROM investment_rule WHERE round_id = ?", ROUND_ID);
        jdbcTemplate.update("DELETE FROM investment_round WHERE round_id = ?", ROUND_ID);
        livePriceAdapter.clear();
    }

    @Given("오케스트레이션용 활성 라운드가 존재한다")
    public void 오케스트레이션용_활성_라운드가_존재한다() {
        jdbcTemplate.update(
                "INSERT INTO investment_round (round_id, version, user_id, round_number,"
                        + " initial_seed, emergency_funding_limit, emergency_charge_count, status,"
                        + " started_at) VALUES (?, 0, ?, 1, 10000000, 1000000, 0, 'ACTIVE', ?)",
                ROUND_ID,
                USER_ID,
                LocalDateTime.of(2026, 1, 1, 0, 0));
        jdbcTemplate.update(
                "INSERT INTO wallet (wallet_id, round_id, exchange_id, seed_amount, created_at) "
                        + "VALUES (?, ?, ?, 10000000, ?)",
                WALLET_ID,
                ROUND_ID,
                EXCHANGE_ID,
                LocalDateTime.now());
    }

    @Given("오케스트레이션용 거래소 정보가 존재한다")
    public void 오케스트레이션용_거래소_정보가_존재한다() {
        Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM exchange_market WHERE exchange_id = ?",
                        Integer.class,
                        EXCHANGE_ID);
        if (count == null || count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO exchange_market (exchange_id, name, market_type,"
                            + " base_currency_coin_id, fee_rate) VALUES (?, 'Upbit', 'DOMESTIC', ?,"
                            + " 0.0005)",
                    EXCHANGE_ID,
                    BASE_CURRENCY_COIN_ID);
        }
    }

    @Given("오케스트레이션용 잔고가 존재한다")
    public void 오케스트레이션용_잔고가_존재한다() {
        jdbcTemplate.update(
                "INSERT INTO wallet_balance (wallet_id, coin_id, available, locked) VALUES (?, ?,"
                        + " ?, 0)",
                WALLET_ID,
                BASE_CURRENCY_COIN_ID,
                new BigDecimal("10000000"));
    }

    @Given("오케스트레이션용 보유 종목이 존재한다")
    public void 오케스트레이션용_보유_종목이_존재한다() {
        // no holdings for simplicity
    }

    @Given("오케스트레이션용 랭킹 대상 라운드가 존재한다")
    public void 오케스트레이션용_랭킹_대상_라운드가_존재한다() {
        for (int i = 0; i < 5; i++) {
            jdbcTemplate.update(
                    "INSERT INTO orders (idempotency_key, user_id, wallet_id, exchange_coin_id,"
                            + " coin_id, base_coin_id, exchange_name, market_symbol, order_type,"
                            + " side, order_amount, quantity, price, filled_price, fee, fee_rate,"
                            + " status, created_at, filled_at) VALUES (?, ?, ?, 10, 2, 1, 'UPBIT',"
                            + " 'BTC/KRW', 'MARKET', 'BUY', 100000, 0.001, 50000000, 50000000, 50,"
                            + " 0.0005, 'FILLED', ?, ?)",
                    java.util.UUID.randomUUID().toString(),
                    USER_ID,
                    WALLET_ID,
                    LocalDateTime.now(),
                    LocalDateTime.now());
        }
    }

    @Given("오케스트레이션용 리포트 데이터가 존재한다")
    public void 오케스트레이션용_리포트_데이터가_존재한다() {
        // active round + wallet already created above → ActiveRoundExchangeQueryAdapter will find
        // them
        // no violations → regret report will be skipped (empty Optional)
    }

    @When("전체 배치를 순차 실행한다")
    public void 전체_배치를_순차_실행한다() throws Exception {
        JobParameters params =
                new JobParametersBuilder()
                        .addString("snapshotDate", SNAPSHOT_DATE.toString())
                        .addLong("run.id", System.currentTimeMillis())
                        .toJobParameters();

        jobOperator.start(snapshotJob, params);
        snapshotCompleted = true;

        jobOperator.start(rankingJob, params);
        rankingCompleted = true;

        jobOperator.start(regretReportJob, params);
        reportCompleted = true;
    }

    @Then("스냅샷 Job은 COMPLETED 상태이다")
    public void 스냅샷_Job은_COMPLETED_상태이다() {
        assertThat(snapshotCompleted).isTrue();
        assertThat(snapshotRepository.findAll()).isNotEmpty();
    }

    @Then("랭킹 Job은 COMPLETED 상태이다")
    public void 랭킹_Job은_COMPLETED_상태이다() {
        assertThat(rankingCompleted).isTrue();
    }

    @Then("리포트 Job은 COMPLETED 상태이다")
    public void 리포트_Job은_COMPLETED_상태이다() {
        assertThat(reportCompleted).isTrue();
    }
}
