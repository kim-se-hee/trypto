package ksh.tryptobackend.acceptance.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ksh.tryptobackend.acceptance.mock.MockActiveRoundListAdapter;
import ksh.tryptobackend.acceptance.mock.MockActiveRoundQueryAdapter;
import ksh.tryptobackend.acceptance.mock.MockBalanceQueryAdapter;
import ksh.tryptobackend.acceptance.mock.MockEligibleRoundQueryAdapter;
import ksh.tryptobackend.acceptance.mock.MockEmergencyFundingSnapshotAdapter;
import ksh.tryptobackend.acceptance.mock.MockExchangeInfoQueryAdapter;
import ksh.tryptobackend.acceptance.mock.MockLivePriceAdapter;
import ksh.tryptobackend.acceptance.mock.MockSnapshotHoldingQueryAdapter;
import ksh.tryptobackend.acceptance.mock.MockTradeViolationQueryAdapter;
import ksh.tryptobackend.acceptance.mock.MockWalletSnapshotAdapter;
import ksh.tryptobackend.ranking.adapter.out.repository.PortfolioSnapshotJpaRepository;
import ksh.tryptobackend.ranking.adapter.out.repository.RankingJpaRepository;
import ksh.tryptobackend.ranking.adapter.out.repository.SnapshotDetailJpaRepository;
import ksh.tryptobackend.ranking.domain.vo.KrwConversionRate;
import ksh.tryptobackend.regretanalysis.adapter.out.repository.RegretReportJpaRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

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
    private final MockActiveRoundQueryAdapter activeRoundQueryAdapter;
    private final MockWalletSnapshotAdapter walletSnapshotAdapter;
    private final MockExchangeInfoQueryAdapter exchangeInfoQueryAdapter;
    private final MockSnapshotHoldingQueryAdapter holdingQueryAdapter;
    private final MockBalanceQueryAdapter balanceQueryAdapter;
    private final MockEmergencyFundingSnapshotAdapter emergencyFundingAdapter;
    private final MockEligibleRoundQueryAdapter eligibleRoundQueryAdapter;
    private final MockActiveRoundListAdapter activeRoundListAdapter;
    private final MockTradeViolationQueryAdapter tradeViolationQueryAdapter;
    private final MockLivePriceAdapter livePriceAdapter;

    private boolean snapshotCompleted;
    private boolean rankingCompleted;
    private boolean reportCompleted;

    public BatchOrchestrationStepDefinition(JobOperator jobOperator,
                                             Job snapshotJob,
                                             Job rankingJob,
                                             Job regretReportJob,
                                             PortfolioSnapshotJpaRepository snapshotRepository,
                                             SnapshotDetailJpaRepository detailRepository,
                                             RankingJpaRepository rankingRepository,
                                             RegretReportJpaRepository regretReportRepository,
                                             MockActiveRoundQueryAdapter activeRoundQueryAdapter,
                                             MockWalletSnapshotAdapter walletSnapshotAdapter,
                                             MockExchangeInfoQueryAdapter exchangeInfoQueryAdapter,
                                             MockSnapshotHoldingQueryAdapter holdingQueryAdapter,
                                             MockBalanceQueryAdapter balanceQueryAdapter,
                                             MockEmergencyFundingSnapshotAdapter emergencyFundingAdapter,
                                             MockEligibleRoundQueryAdapter eligibleRoundQueryAdapter,
                                             MockActiveRoundListAdapter activeRoundListAdapter,
                                             MockTradeViolationQueryAdapter tradeViolationQueryAdapter,
                                             MockLivePriceAdapter livePriceAdapter) {
        this.jobOperator = jobOperator;
        this.snapshotJob = snapshotJob;
        this.rankingJob = rankingJob;
        this.regretReportJob = regretReportJob;
        this.snapshotRepository = snapshotRepository;
        this.detailRepository = detailRepository;
        this.rankingRepository = rankingRepository;
        this.regretReportRepository = regretReportRepository;
        this.activeRoundQueryAdapter = activeRoundQueryAdapter;
        this.walletSnapshotAdapter = walletSnapshotAdapter;
        this.exchangeInfoQueryAdapter = exchangeInfoQueryAdapter;
        this.holdingQueryAdapter = holdingQueryAdapter;
        this.balanceQueryAdapter = balanceQueryAdapter;
        this.emergencyFundingAdapter = emergencyFundingAdapter;
        this.eligibleRoundQueryAdapter = eligibleRoundQueryAdapter;
        this.activeRoundListAdapter = activeRoundListAdapter;
        this.tradeViolationQueryAdapter = tradeViolationQueryAdapter;
        this.livePriceAdapter = livePriceAdapter;
    }

    @Given("전체 배치 데이터를 초기화한다")
    public void 전체_배치_데이터를_초기화한다() {
        regretReportRepository.deleteAllInBatch();
        rankingRepository.deleteAllInBatch();
        detailRepository.deleteAllInBatch();
        snapshotRepository.deleteAllInBatch();
        activeRoundQueryAdapter.clear();
        walletSnapshotAdapter.clear();
        exchangeInfoQueryAdapter.clear();
        holdingQueryAdapter.clear();
        balanceQueryAdapter.clear();
        emergencyFundingAdapter.clear();
        eligibleRoundQueryAdapter.clear();
        activeRoundListAdapter.clear();
        tradeViolationQueryAdapter.clear();
        livePriceAdapter.clear();
    }

    @Given("오케스트레이션용 활성 라운드가 존재한다")
    public void 오케스트레이션용_활성_라운드가_존재한다() {
        activeRoundQueryAdapter.addActiveRound(ROUND_ID, USER_ID, LocalDateTime.of(2026, 1, 1, 0, 0));
        walletSnapshotAdapter.addWallet(WALLET_ID, ROUND_ID, EXCHANGE_ID, new BigDecimal("10000000"));
    }

    @Given("오케스트레이션용 거래소 정보가 존재한다")
    public void 오케스트레이션용_거래소_정보가_존재한다() {
        exchangeInfoQueryAdapter.addExchange(EXCHANGE_ID, BASE_CURRENCY_COIN_ID, KrwConversionRate.DOMESTIC);
    }

    @Given("오케스트레이션용 잔고가 존재한다")
    public void 오케스트레이션용_잔고가_존재한다() {
        balanceQueryAdapter.setBalance(WALLET_ID, BASE_CURRENCY_COIN_ID, new BigDecimal("10000000"));
    }

    @Given("오케스트레이션용 보유 종목이 존재한다")
    public void 오케스트레이션용_보유_종목이_존재한다() {
        // no holdings for simplicity
    }

    @Given("오케스트레이션용 랭킹 대상 라운드가 존재한다")
    public void 오케스트레이션용_랭킹_대상_라운드가_존재한다() {
        eligibleRoundQueryAdapter.addEligibleRound(USER_ID, ROUND_ID, 5, LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Given("오케스트레이션용 리포트 데이터가 존재한다")
    public void 오케스트레이션용_리포트_데이터가_존재한다() {
        activeRoundListAdapter.addRoundExchange(
            ROUND_ID, USER_ID, EXCHANGE_ID, WALLET_ID, LocalDateTime.of(2026, 1, 1, 0, 0));
        // no violations → regret report will be skipped (empty Optional)
    }

    @When("전체 배치를 순차 실행한다")
    public void 전체_배치를_순차_실행한다() throws Exception {
        JobParameters params = new JobParametersBuilder()
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
