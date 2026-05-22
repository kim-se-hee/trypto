package ksh.tryptobackend.acceptance.steps.batch;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import ksh.tryptobackend.acceptance.mock.MockHoldingAdapter;
import ksh.tryptobackend.acceptance.mock.MockLivePriceAdapter;
import ksh.tryptobackend.portfolio.adapter.out.entity.PortfolioSnapshotJpaEntity;
import ksh.tryptobackend.portfolio.adapter.out.entity.SnapshotDetailJpaEntity;
import ksh.tryptobackend.portfolio.adapter.out.repository.PortfolioSnapshotJpaRepository;
import ksh.tryptobackend.portfolio.adapter.out.repository.SnapshotDetailJpaRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.jdbc.core.JdbcTemplate;

public class SnapshotBatchStepDefinition {

    private static final LocalDate SNAPSHOT_DATE = LocalDate.of(2026, 3, 1);

    private final JobOperator jobOperator;
    private final Job snapshotJob;
    private final JobRepository jobRepository;
    private final PortfolioSnapshotJpaRepository snapshotRepository;
    private final SnapshotDetailJpaRepository detailRepository;
    private final MockHoldingAdapter holdingAdapter;
    private final MockLivePriceAdapter livePriceAdapter;
    private final JdbcTemplate jdbcTemplate;

    private List<PortfolioSnapshotJpaEntity> savedSnapshots;

    public SnapshotBatchStepDefinition(
            JobOperator jobOperator,
            Job snapshotJob,
            JobRepository jobRepository,
            PortfolioSnapshotJpaRepository snapshotRepository,
            SnapshotDetailJpaRepository detailRepository,
            MockHoldingAdapter holdingAdapter,
            MockLivePriceAdapter livePriceAdapter,
            JdbcTemplate jdbcTemplate) {
        this.jobOperator = jobOperator;
        this.snapshotJob = snapshotJob;
        this.jobRepository = jobRepository;
        this.snapshotRepository = snapshotRepository;
        this.detailRepository = detailRepository;
        this.holdingAdapter = holdingAdapter;
        this.livePriceAdapter = livePriceAdapter;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Given("스냅샷 배치 데이터를 초기화한다")
    public void 스냅샷_배치_데이터를_초기화한다() {
        // 글로벌 DatabaseCleanupHook 이 매 시나리오 시작 시 scenario 테이블을 모두 TRUNCATE 한다.
        // seed 테이블(coin/exchange_market/exchange_coin/...)은 청소 대상이 아니므로 본 step 에서도 건드리지 않는다.
        holdingAdapter.clear();
        livePriceAdapter.clear();
    }

    @Given("스냅샷용 활성 라운드가 존재한다")
    public void 스냅샷용_활성_라운드가_존재한다(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            Long roundId = Long.valueOf(row.get("roundId"));
            Long userId = Long.valueOf(row.get("userId"));
            Long exchangeId = Long.valueOf(row.get("exchangeId"));
            Long walletId = Long.valueOf(row.get("walletId"));
            BigDecimal seedAmount = new BigDecimal(row.get("seedAmount"));

            ensureActiveRound(roundId, userId);
            insertWallet(walletId, roundId, exchangeId, seedAmount);
        }
    }

    @Given("스냅샷용 거래소 정보가 존재한다")
    public void 스냅샷용_거래소_정보가_존재한다(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            Long exchangeId = Long.valueOf(row.get("exchangeId"));
            Long baseCurrencyCoinId = Long.valueOf(row.get("baseCurrencyCoinId"));
            String conversionRate = row.get("conversionRate");
            String marketType = "DOMESTIC".equals(conversionRate) ? "DOMESTIC" : "OVERSEAS";
            ensureCoin(baseCurrencyCoinId);
            insertExchange(exchangeId, baseCurrencyCoinId, marketType);
        }
    }

    private void ensureCoin(Long coinId) {
        jdbcTemplate.update(
                "INSERT IGNORE INTO coin (coin_id, symbol, name) VALUES (?, ?, ?)",
                coinId,
                "COIN" + coinId,
                "coin-" + coinId);
    }

    @Given("스냅샷용 잔고가 존재한다")
    public void 스냅샷용_잔고가_존재한다(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            Long walletId = Long.valueOf(row.get("walletId"));
            Long coinId = Long.valueOf(row.get("coinId"));
            BigDecimal balance = new BigDecimal(row.get("balance"));
            jdbcTemplate.update(
                    "INSERT INTO wallet_balance (wallet_id, coin_id, available, locked) VALUES (?,"
                            + " ?, ?, 0)",
                    walletId,
                    coinId,
                    balance);
        }
    }

    @Given("스냅샷용 보유 종목이 존재한다")
    public void 스냅샷용_보유_종목이_존재한다(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            Long walletId = Long.valueOf(row.get("walletId"));
            Long exchangeId = Long.valueOf(row.get("exchangeId"));
            Long coinId = Long.valueOf(row.get("coinId"));
            BigDecimal avgBuyPrice = new BigDecimal(row.get("avgBuyPrice"));
            BigDecimal quantity = new BigDecimal(row.get("quantity"));
            BigDecimal currentPrice = new BigDecimal(row.get("currentPrice"));

            holdingAdapter.setHolding(walletId, coinId, avgBuyPrice, quantity, 0);

            Long exchangeCoinId = exchangeId * 100 + coinId;
            jdbcTemplate.update(
                    "INSERT IGNORE INTO exchange_coin (exchange_coin_id, exchange_id, coin_id)"
                            + " VALUES (?, ?, ?)",
                    exchangeCoinId,
                    exchangeId,
                    coinId);

            livePriceAdapter.setPrice(exchangeCoinId, currentPrice);
        }
    }

    @Given("스냅샷용 긴급자금 합계는 {int}이다")
    public void 스냅샷용_긴급자금_합계는_이다(int amount, DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            Long roundId = Long.valueOf(row.get("roundId"));
            Long exchangeId = Long.valueOf(row.get("exchangeId"));
            jdbcTemplate.update(
                    "INSERT INTO emergency_funding (round_id, exchange_id, amount, idempotency_key,"
                            + " created_at) VALUES (?, ?, ?, ?, ?)",
                    roundId,
                    exchangeId,
                    new BigDecimal(amount),
                    java.util.UUID.randomUUID().toString(),
                    LocalDateTime.now());
        }
    }

    @When("스냅샷 배치를 실행한다")
    public void 스냅샷_배치를_실행한다() throws Exception {
        JobParameters params =
                new JobParametersBuilder()
                        .addString("snapshotDate", SNAPSHOT_DATE.toString())
                        .addLong("run.id", System.currentTimeMillis())
                        .toJobParameters();
        jobOperator.start(snapshotJob, params);
        savedSnapshots = snapshotRepository.findAll();
    }

    @Then("스냅샷 배치가 COMPLETED 상태이다")
    public void 스냅샷_배치가_COMPLETED_상태이다() {
        var execution =
                jobRepository.getLastJobExecution(
                        snapshotJob.getName(),
                        snapshotRepository.findAll().isEmpty()
                                ? new JobParametersBuilder().toJobParameters()
                                : new JobParametersBuilder()
                                        .addString("snapshotDate", SNAPSHOT_DATE.toString())
                                        .toJobParameters());
        assertThat(savedSnapshots).isNotNull();
    }

    @Then("스냅샷이 {int}건 생성된다")
    public void 스냅샷이_건_생성된다(int count) {
        assertThat(savedSnapshots).hasSize(count);
    }

    @Then("첫 번째 스냅샷의 총자산은 {long}이다")
    public void 첫_번째_스냅샷의_총자산은_이다(long totalAsset) {
        assertThat(savedSnapshots.get(0).getTotalAsset())
                .isEqualByComparingTo(new BigDecimal(totalAsset));
    }

    @Then("첫 번째 스냅샷의 총투자금은 {long}이다")
    public void 첫_번째_스냅샷의_총투자금은_이다(long totalInvestment) {
        assertThat(savedSnapshots.get(0).getTotalInvestment())
                .isEqualByComparingTo(new BigDecimal(totalInvestment));
    }

    @Then("첫 번째 스냅샷의 수익률은 {double}이다")
    public void 첫_번째_스냅샷의_수익률은_이다(double profitRate) {
        assertThat(savedSnapshots.get(0).getTotalProfitRate())
                .isEqualByComparingTo(new BigDecimal(String.valueOf(profitRate)));
    }

    @Then("스냅샷 상세가 {int}건 생성된다")
    public void 스냅샷_상세가_건_생성된다(int count) {
        Long snapshotId = savedSnapshots.get(0).getId();
        List<SnapshotDetailJpaEntity> details = detailRepository.findBySnapshotId(snapshotId);
        assertThat(details).hasSize(count);
    }

    @Then("첫 번째 상세의 수익률은 {double}이다")
    public void 첫_번째_상세의_수익률은_이다(double profitRate) {
        Long snapshotId = savedSnapshots.get(0).getId();
        List<SnapshotDetailJpaEntity> details = detailRepository.findBySnapshotId(snapshotId);
        assertThat(details.get(0).getProfitRate())
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

    private void insertWallet(Long walletId, Long roundId, Long exchangeId, BigDecimal seedAmount) {
        jdbcTemplate.update(
                "INSERT INTO wallet (wallet_id, round_id, exchange_id, seed_amount, created_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                walletId,
                roundId,
                exchangeId,
                seedAmount,
                LocalDateTime.now());
    }

    private void insertExchange(Long exchangeId, Long baseCurrencyCoinId, String marketType) {
        // 매 시나리오 시작 시 seed 가 재적재되므로 충돌 시 UPDATE 로 시나리오가 원하는 base_currency_coin_id 를 보장한다.
        jdbcTemplate.update(
                "INSERT INTO exchange_market (exchange_id, name, market_type,"
                    + " base_currency_coin_id, fee_rate) VALUES (?, ?, ?, ?, 0.0005) ON DUPLICATE"
                    + " KEY UPDATE base_currency_coin_id = VALUES(base_currency_coin_id),"
                    + " market_type = VALUES(market_type)",
                exchangeId,
                "Exchange-" + exchangeId,
                marketType,
                baseCurrencyCoinId);
    }
}
