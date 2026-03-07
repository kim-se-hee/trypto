package ksh.tryptobackend.portfolio.domain.model;

import ksh.tryptobackend.portfolio.domain.vo.KrwConversionRate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioSnapshotTest {

    private static final Long USER_ID = 1L;
    private static final Long ROUND_ID = 1L;
    private static final Long EXCHANGE_ID = 1L;
    private static final LocalDate SNAPSHOT_DATE = LocalDate.of(2026, 3, 1);

    @Test
    @DisplayName("수익 발생 시 양수 수익률을 계산한다")
    void create_profit_calculatesPositiveProfit() {
        BigDecimal totalAsset = new BigDecimal("11000000");
        BigDecimal totalInvestment = new BigDecimal("10000000");

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(
            USER_ID, ROUND_ID, EXCHANGE_ID,
            totalAsset, totalInvestment,
            KrwConversionRate.DOMESTIC, SNAPSHOT_DATE, List.of());

        assertThat(snapshot.getTotalProfit()).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(snapshot.getTotalProfitRate()).isEqualByComparingTo(new BigDecimal("10.0000"));
    }

    @Test
    @DisplayName("손실 발생 시 음수 수익률을 계산한다")
    void create_loss_calculatesNegativeProfit() {
        BigDecimal totalAsset = new BigDecimal("9750000");
        BigDecimal totalInvestment = new BigDecimal("10000000");

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(
            USER_ID, ROUND_ID, EXCHANGE_ID,
            totalAsset, totalInvestment,
            KrwConversionRate.DOMESTIC, SNAPSHOT_DATE, List.of());

        assertThat(snapshot.getTotalProfit()).isEqualByComparingTo(new BigDecimal("-250000"));
        assertThat(snapshot.getTotalProfitRate()).isEqualByComparingTo(new BigDecimal("-2.5000"));
    }

    @Test
    @DisplayName("국내 거래소는 환율 1을 적용한다")
    void create_domestic_appliesConversionRate1() {
        BigDecimal totalAsset = new BigDecimal("5000000");
        BigDecimal totalInvestment = new BigDecimal("5000000");

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(
            USER_ID, ROUND_ID, EXCHANGE_ID,
            totalAsset, totalInvestment,
            KrwConversionRate.DOMESTIC, SNAPSHOT_DATE, List.of());

        assertThat(snapshot.getTotalAssetKrw()).isEqualByComparingTo(new BigDecimal("5000000"));
        assertThat(snapshot.getTotalInvestmentKrw()).isEqualByComparingTo(new BigDecimal("5000000"));
    }

    @Test
    @DisplayName("해외 거래소는 환율 1400을 적용한다")
    void create_overseas_appliesConversionRate1400() {
        BigDecimal totalAsset = new BigDecimal("1000");
        BigDecimal totalInvestment = new BigDecimal("1000");

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(
            USER_ID, ROUND_ID, EXCHANGE_ID,
            totalAsset, totalInvestment,
            KrwConversionRate.OVERSEAS, SNAPSHOT_DATE, List.of());

        assertThat(snapshot.getTotalAssetKrw()).isEqualByComparingTo(new BigDecimal("1400000"));
        assertThat(snapshot.getTotalInvestmentKrw()).isEqualByComparingTo(new BigDecimal("1400000"));
    }

    @Test
    @DisplayName("투자금이 0이면 수익률 0을 반환한다")
    void create_zeroInvestment_returnsZeroProfitRate() {
        BigDecimal totalAsset = new BigDecimal("5000000");
        BigDecimal totalInvestment = BigDecimal.ZERO;

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(
            USER_ID, ROUND_ID, EXCHANGE_ID,
            totalAsset, totalInvestment,
            KrwConversionRate.DOMESTIC, SNAPSHOT_DATE, List.of());

        assertThat(snapshot.getTotalProfitRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
