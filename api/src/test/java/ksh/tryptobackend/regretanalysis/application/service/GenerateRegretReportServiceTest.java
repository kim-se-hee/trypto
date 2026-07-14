package ksh.tryptobackend.regretanalysis.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.command.GenerateRegretReportCommand;
import ksh.tryptobackend.regretanalysis.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.PortfolioQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.TradingQueryPort;
import ksh.tryptobackend.regretanalysis.domain.model.AssetSnapshot;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.ViolatedOrders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GenerateRegretReportServiceTest {

    private static final Long ROUND_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long EXCHANGE_ID = 100L;
    private static final Long WALLET_ID = 1000L;

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    private static final GenerateRegretReportCommand COMMAND = new GenerateRegretReportCommand(
            ROUND_ID, USER_ID, EXCHANGE_ID, WALLET_ID, LocalDateTime.of(2026, 7, 1, 9, 0));

    private TradingQueryPort tradingQueryPort;
    private MarketDataQueryPort marketDataQueryPort;
    private PortfolioQueryPort portfolioQueryPort;
    private GenerateRegretReportService generateRegretReportService;

    @BeforeEach
    void setUp() {
        tradingQueryPort = mock(TradingQueryPort.class);
        marketDataQueryPort = mock(MarketDataQueryPort.class);
        portfolioQueryPort = mock(PortfolioQueryPort.class);
        generateRegretReportService =
                new GenerateRegretReportService(tradingQueryPort, marketDataQueryPort, portfolioQueryPort, FIXED_CLOCK);
    }

    @Test
    @DisplayName("위반이 없어도 스냅샷이 있으면 위반 0건짜리 리포트를 생성한다")
    void generateReport_noViolations_returnsEmptyReport() {
        when(portfolioQueryPort.findLatestSnapshot(ROUND_ID, EXCHANGE_ID)).thenReturn(Optional.of(snapshot()));
        when(tradingQueryPort.findViolatedOrders(ROUND_ID, EXCHANGE_ID, WALLET_ID))
                .thenReturn(new ViolatedOrders(List.of()));

        Optional<RegretReport> report = generateRegretReportService.generateReport(COMMAND);

        assertThat(report).isPresent();
        assertThat(report.get().getTotalViolations()).isZero();
        assertThat(report.get().getMissedProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.get().getRuleImpacts()).isEmpty();
        assertThat(report.get().getViolationDetails().toList()).isEmpty();
    }

    @Test
    @DisplayName("위반이 없으면 원칙 준수 수익률은 실제 수익률과 같다")
    void generateReport_noViolations_ruleFollowedRateEqualsActualRate() {
        when(portfolioQueryPort.findLatestSnapshot(ROUND_ID, EXCHANGE_ID)).thenReturn(Optional.of(snapshot()));
        when(tradingQueryPort.findViolatedOrders(ROUND_ID, EXCHANGE_ID, WALLET_ID))
                .thenReturn(new ViolatedOrders(List.of()));

        RegretReport report =
                generateRegretReportService.generateReport(COMMAND).orElseThrow();

        assertThat(report.getRuleFollowedProfitRate()).isEqualByComparingTo(report.getActualProfitRate());
    }

    @Test
    @DisplayName("스냅샷이 없으면 리포트를 생성하지 않는다")
    void generateReport_noSnapshot_returnsEmpty() {
        when(portfolioQueryPort.findLatestSnapshot(ROUND_ID, EXCHANGE_ID)).thenReturn(Optional.empty());

        Optional<RegretReport> report = generateRegretReportService.generateReport(COMMAND);

        assertThat(report).isEmpty();
        verify(tradingQueryPort, never()).findViolatedOrders(any(), any(), any());
    }

    private AssetSnapshot snapshot() {
        return AssetSnapshot.reconstitute(
                1L,
                ROUND_ID,
                EXCHANGE_ID,
                new BigDecimal("5200000"),
                new BigDecimal("5000000"),
                new BigDecimal("4.00"),
                LocalDate.of(2026, 7, 14));
    }
}
