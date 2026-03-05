package ksh.tryptobackend.acceptance.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ksh.tryptobackend.acceptance.mock.MockActiveRoundQueryAdapter;
import ksh.tryptobackend.acceptance.mock.MockBalanceQueryAdapter;
import ksh.tryptobackend.acceptance.mock.MockEmergencyFundingSnapshotAdapter;
import ksh.tryptobackend.acceptance.mock.MockExchangeInfoQueryAdapter;
import ksh.tryptobackend.acceptance.mock.MockSnapshotHoldingQueryAdapter;
import ksh.tryptobackend.acceptance.mock.MockWalletSnapshotAdapter;
import ksh.tryptobackend.ranking.adapter.out.entity.PortfolioSnapshotJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.entity.SnapshotDetailJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.repository.PortfolioSnapshotJpaRepository;
import ksh.tryptobackend.ranking.adapter.out.repository.SnapshotDetailJpaRepository;
import ksh.tryptobackend.ranking.domain.vo.KrwConversionRate;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SnapshotBatchStepDefinition {

    private static final LocalDate SNAPSHOT_DATE = LocalDate.of(2026, 3, 1);

    private final JobOperator jobOperator;
    private final Job snapshotJob;
    private final JobRepository jobRepository;
    private final PortfolioSnapshotJpaRepository snapshotRepository;
    private final SnapshotDetailJpaRepository detailRepository;
    private final MockActiveRoundQueryAdapter activeRoundQueryAdapter;
    private final MockWalletSnapshotAdapter walletSnapshotAdapter;
    private final MockExchangeInfoQueryAdapter exchangeInfoQueryAdapter;
    private final MockSnapshotHoldingQueryAdapter holdingQueryAdapter;
    private final MockBalanceQueryAdapter balanceQueryAdapter;
    private final MockEmergencyFundingSnapshotAdapter emergencyFundingAdapter;

    private List<PortfolioSnapshotJpaEntity> savedSnapshots;

    public SnapshotBatchStepDefinition(JobOperator jobOperator,
                                        Job snapshotJob,
                                        JobRepository jobRepository,
                                        PortfolioSnapshotJpaRepository snapshotRepository,
                                        SnapshotDetailJpaRepository detailRepository,
                                        MockActiveRoundQueryAdapter activeRoundQueryAdapter,
                                        MockWalletSnapshotAdapter walletSnapshotAdapter,
                                        MockExchangeInfoQueryAdapter exchangeInfoQueryAdapter,
                                        MockSnapshotHoldingQueryAdapter holdingQueryAdapter,
                                        MockBalanceQueryAdapter balanceQueryAdapter,
                                        MockEmergencyFundingSnapshotAdapter emergencyFundingAdapter) {
        this.jobOperator = jobOperator;
        this.snapshotJob = snapshotJob;
        this.jobRepository = jobRepository;
        this.snapshotRepository = snapshotRepository;
        this.detailRepository = detailRepository;
        this.activeRoundQueryAdapter = activeRoundQueryAdapter;
        this.walletSnapshotAdapter = walletSnapshotAdapter;
        this.exchangeInfoQueryAdapter = exchangeInfoQueryAdapter;
        this.holdingQueryAdapter = holdingQueryAdapter;
        this.balanceQueryAdapter = balanceQueryAdapter;
        this.emergencyFundingAdapter = emergencyFundingAdapter;
    }

    @Given("스냅샷 배치 데이터를 초기화한다")
    public void 스냅샷_배치_데이터를_초기화한다() {
        detailRepository.deleteAllInBatch();
        snapshotRepository.deleteAllInBatch();
        activeRoundQueryAdapter.clear();
        walletSnapshotAdapter.clear();
        exchangeInfoQueryAdapter.clear();
        holdingQueryAdapter.clear();
        balanceQueryAdapter.clear();
        emergencyFundingAdapter.clear();
    }

    @Given("스냅샷용 활성 라운드가 존재한다")
    public void 스냅샷용_활성_라운드가_존재한다(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            Long roundId = Long.valueOf(row.get("roundId"));
            Long userId = Long.valueOf(row.get("userId"));
            Long exchangeId = Long.valueOf(row.get("exchangeId"));
            Long walletId = Long.valueOf(row.get("walletId"));
            BigDecimal seedAmount = new BigDecimal(row.get("seedAmount"));

            activeRoundQueryAdapter.addActiveRound(roundId, userId, LocalDateTime.of(2026, 1, 1, 0, 0));
            walletSnapshotAdapter.addWallet(walletId, roundId, exchangeId, seedAmount);
        }
    }

    @Given("스냅샷용 거래소 정보가 존재한다")
    public void 스냅샷용_거래소_정보가_존재한다(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            Long exchangeId = Long.valueOf(row.get("exchangeId"));
            Long baseCurrencyCoinId = Long.valueOf(row.get("baseCurrencyCoinId"));
            KrwConversionRate rate = KrwConversionRate.valueOf(row.get("conversionRate"));
            exchangeInfoQueryAdapter.addExchange(exchangeId, baseCurrencyCoinId, rate);
        }
    }

    @Given("스냅샷용 잔고가 존재한다")
    public void 스냅샷용_잔고가_존재한다(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            Long walletId = Long.valueOf(row.get("walletId"));
            Long coinId = Long.valueOf(row.get("coinId"));
            BigDecimal balance = new BigDecimal(row.get("balance"));
            balanceQueryAdapter.setBalance(walletId, coinId, balance);
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
            holdingQueryAdapter.addHolding(walletId, exchangeId, coinId, avgBuyPrice, quantity, currentPrice);
        }
    }

    @Given("스냅샷용 긴급자금 합계는 {int}이다")
    public void 스냅샷용_긴급자금_합계는_이다(int amount, DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            Long roundId = Long.valueOf(row.get("roundId"));
            Long exchangeId = Long.valueOf(row.get("exchangeId"));
            emergencyFundingAdapter.setFunding(roundId, exchangeId, new BigDecimal(amount));
        }
    }

    @When("스냅샷 배치를 실행한다")
    public void 스냅샷_배치를_실행한다() throws Exception {
        JobParameters params = new JobParametersBuilder()
            .addString("snapshotDate", SNAPSHOT_DATE.toString())
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters();
        jobOperator.start(snapshotJob, params);
        savedSnapshots = snapshotRepository.findAll();
    }

    @Then("스냅샷 배치가 COMPLETED 상태이다")
    public void 스냅샷_배치가_COMPLETED_상태이다() {
        var execution = jobRepository.getLastJobExecution(
            snapshotJob.getName(), snapshotRepository.findAll().isEmpty()
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
}
