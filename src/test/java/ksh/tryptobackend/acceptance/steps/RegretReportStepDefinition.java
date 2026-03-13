package ksh.tryptobackend.acceptance.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ksh.tryptobackend.acceptance.testclient.CommonApiClient;
import ksh.tryptobackend.investmentround.adapter.out.entity.InvestmentRoundJpaEntity;
import ksh.tryptobackend.investmentround.adapter.out.repository.InvestmentRoundJpaRepository;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeJpaRepository;
import ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType;
import ksh.tryptobackend.regretanalysis.adapter.out.entity.RegretReportJpaEntity;
import ksh.tryptobackend.regretanalysis.adapter.out.repository.RegretReportJpaRepository;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.RuleImpact;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import ksh.tryptobackend.regretanalysis.domain.vo.ImpactGap;
import ksh.tryptobackend.wallet.adapter.out.entity.WalletJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletJpaRepository;
import ksh.tryptobackend.wallet.domain.model.Wallet;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RegretReportStepDefinition {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long EXCHANGE_ID_WITH_REPORT = 1L;
    private static final Long EXCHANGE_ID_WITHOUT_REPORT = 2L;
    private static final Long EXCHANGE_ID_WITHOUT_WALLET = 99L;
    private static final Long KRW_COIN_ID = 1L;
    private static final Long BTC_COIN_ID = 2L;

    private final CommonApiClient apiClient;
    private final JdbcTemplate jdbcTemplate;
    private final RegretReportJpaRepository regretReportJpaRepository;
    private final ExchangeJpaRepository exchangeJpaRepository;
    private final InvestmentRoundJpaRepository investmentRoundJpaRepository;
    private final WalletJpaRepository walletJpaRepository;

    private Long savedRoundId;
    private Long savedRuleId;

    public RegretReportStepDefinition(
            CommonApiClient apiClient,
            JdbcTemplate jdbcTemplate,
            RegretReportJpaRepository regretReportJpaRepository,
            ExchangeJpaRepository exchangeJpaRepository,
            InvestmentRoundJpaRepository investmentRoundJpaRepository,
            WalletJpaRepository walletJpaRepository) {
        this.apiClient = apiClient;
        this.jdbcTemplate = jdbcTemplate;
        this.regretReportJpaRepository = regretReportJpaRepository;
        this.exchangeJpaRepository = exchangeJpaRepository;
        this.investmentRoundJpaRepository = investmentRoundJpaRepository;
        this.walletJpaRepository = walletJpaRepository;
    }

    @Before
    public void setUp() {
        regretReportJpaRepository.deleteAll();
        walletJpaRepository.deleteAllInBatch();
        investmentRoundJpaRepository.deleteAll();
        exchangeJpaRepository.deleteAllInBatch();
        jdbcTemplate.execute("DELETE FROM `coin`");
    }

    @Given("복기 리포트용 기초 데이터가 준비되어 있다")
    public void 복기_리포트용_기초_데이터가_준비되어_있다() {
        insertCoins();
        insertExchanges();
        savedRoundId = insertRound();
        insertWallets(savedRoundId);
    }

    @Given("복기 리포트가 생성되어 있다")
    public void 복기_리포트가_생성되어_있다() {
        savedRuleId = insertRuleViaCascade(savedRoundId);
        insertReport(savedRoundId, savedRuleId);
    }

    @When("복기 리포트 조회 요청을 보낸다")
    public void 복기_리포트_조회_요청을_보낸다() {
        apiClient.get(regretReportUrl(savedRoundId, USER_ID, EXCHANGE_ID_WITH_REPORT));
    }

    @When("다른 유저로 복기 리포트 조회 요청을 보낸다")
    public void 다른_유저로_복기_리포트_조회_요청을_보낸다() {
        apiClient.get(regretReportUrl(savedRoundId, OTHER_USER_ID, EXCHANGE_ID_WITH_REPORT));
    }

    @When("지갑이 없는 거래소로 복기 리포트 조회 요청을 보낸다")
    public void 지갑이_없는_거래소로_복기_리포트_조회_요청을_보낸다() {
        apiClient.get(regretReportUrl(savedRoundId, USER_ID, EXCHANGE_ID_WITHOUT_WALLET));
    }

    @When("리포트가 없는 거래소로 복기 리포트 조회 요청을 보낸다")
    public void 리포트가_없는_거래소로_복기_리포트_조회_요청을_보낸다() {
        apiClient.get(regretReportUrl(savedRoundId, USER_ID, EXCHANGE_ID_WITHOUT_REPORT));
    }

    @Then("리포트의 거래소명은 {string}이다")
    public void 리포트의_거래소명은_이다(String exchangeName) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.exchangeName").isEqualTo(exchangeName);
    }

    @Then("리포트의 통화는 {string}이다")
    public void 리포트의_통화는_이다(String currency) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.currency").isEqualTo(currency);
    }

    @Then("리포트의 위반 건수는 {int}이다")
    public void 리포트의_위반_건수는_이다(int count) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.totalViolations").isEqualTo(count);
    }

    @Then("규칙별 영향도는 {int}개이다")
    public void 규칙별_영향도는_개이다(int count) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.ruleImpacts.length()").isEqualTo(count);
    }

    @Then("위반 상세는 {int}개이다")
    public void 위반_상세는_개이다(int count) {
        apiClient.getLastResponse()
            .expectBody()
            .jsonPath("$.data.violationDetails.length()").isEqualTo(count);
    }

    private String regretReportUrl(Long roundId, Long userId, Long exchangeId) {
        return "/api/rounds/" + roundId + "/regret?userId=" + userId + "&exchangeId=" + exchangeId;
    }

    private void insertCoins() {
        jdbcTemplate.update("INSERT INTO `coin` (`coin_id`, `symbol`, `name`) VALUES (?, ?, ?)", KRW_COIN_ID, "KRW", "Korean Won");
        jdbcTemplate.update("INSERT INTO `coin` (`coin_id`, `symbol`, `name`) VALUES (?, ?, ?)", BTC_COIN_ID, "BTC", "Bitcoin");
    }

    private void insertExchanges() {
        exchangeJpaRepository.saveAll(List.of(
            new ExchangeJpaEntity(EXCHANGE_ID_WITH_REPORT, "UPBIT", ExchangeMarketType.DOMESTIC, KRW_COIN_ID, new BigDecimal("0.0005")),
            new ExchangeJpaEntity(EXCHANGE_ID_WITHOUT_REPORT, "BITHUMB", ExchangeMarketType.DOMESTIC, KRW_COIN_ID, new BigDecimal("0.0005"))
        ));
    }

    private Long insertRound() {
        InvestmentRound round = InvestmentRound.reconstitute(
            null, null, USER_ID, 1,
            new BigDecimal("5000000"), new BigDecimal("500000"), 3,
            RoundStatus.ACTIVE,
            LocalDateTime.of(2025, 1, 1, 0, 0), null,
            List.of(), List.of());
        return investmentRoundJpaRepository.save(InvestmentRoundJpaEntity.fromDomain(round)).getId();
    }

    private void insertWallets(Long roundId) {
        walletJpaRepository.save(WalletJpaEntity.fromDomain(
            Wallet.create(roundId, EXCHANGE_ID_WITH_REPORT, BigDecimal.ZERO, LocalDateTime.now())));
        walletJpaRepository.save(WalletJpaEntity.fromDomain(
            Wallet.create(roundId, EXCHANGE_ID_WITHOUT_REPORT, BigDecimal.ZERO, LocalDateTime.now())));
    }

    private Long insertRuleViaCascade(Long roundId) {
        jdbcTemplate.update(
            "INSERT INTO investment_rule (round_id, rule_type, threshold_value, created_at) VALUES (?, ?, ?, ?)",
            roundId, "LOSS_CUT", new BigDecimal("10"), LocalDateTime.now());
        return jdbcTemplate.queryForObject(
            "SELECT rule_id FROM investment_rule WHERE round_id = ? AND rule_type = ?",
            Long.class, roundId, "LOSS_CUT");
    }

    private void insertReport(Long roundId, Long ruleId) {
        LocalDateTime now = LocalDateTime.of(2025, 1, 15, 12, 0);
        RegretReport report = RegretReport.reconstitute(
            null, USER_ID, roundId, EXCHANGE_ID_WITH_REPORT,
            3, new BigDecimal("150000"),
            new BigDecimal("-5.25"), new BigDecimal("2.30"),
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15),
            now,
            List.of(RuleImpact.create(ruleId, 3, new BigDecimal("50000"), ImpactGap.of(new BigDecimal("7.55")))),
            List.of(
                ViolationDetail.create(100L, ruleId, BTC_COIN_ID,
                    new BigDecimal("30000"), new BigDecimal("-15000"), now.minusDays(5)),
                ViolationDetail.create(null, ruleId, BTC_COIN_ID,
                    new BigDecimal("20000"), new BigDecimal("-10000"), now.minusDays(3))
            )
        );
        regretReportJpaRepository.save(RegretReportJpaEntity.fromDomain(report));
    }
}
