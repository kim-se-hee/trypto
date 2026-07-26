package ksh.tryptobackend.portfolio.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ksh.tryptobackend.portfolio.domain.vo.KrwConversionRate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PortfolioSnapshotTest {

    private static final Long USER_ID = 1L;
    private static final Long ROUND_ID = 1L;
    private static final Long EXCHANGE_ID = 1L;
    private static final LocalDate SNAPSHOT_DATE = LocalDate.of(2026, 3, 1);

    @Test
    @DisplayName("국내 거래소는 환율 1을 적용한다")
    void create_domestic_appliesConversionRate1() {
        BigDecimal totalAsset = new BigDecimal("5000000");

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(
                USER_ID, ROUND_ID, EXCHANGE_ID, totalAsset, KrwConversionRate.DOMESTIC, SNAPSHOT_DATE, List.of());

        assertThat(snapshot.getTotalAsset()).isEqualByComparingTo(new BigDecimal("5000000"));
        assertThat(snapshot.getTotalAssetKrw()).isEqualByComparingTo(new BigDecimal("5000000"));
    }

    @Test
    @DisplayName("해외 거래소는 환율 1400을 적용한다")
    void create_overseas_appliesConversionRate1400() {
        BigDecimal totalAsset = new BigDecimal("1000");

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(
                USER_ID, ROUND_ID, EXCHANGE_ID, totalAsset, KrwConversionRate.OVERSEAS, SNAPSHOT_DATE, List.of());

        assertThat(snapshot.getTotalAsset()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(snapshot.getTotalAssetKrw()).isEqualByComparingTo(new BigDecimal("1400000"));
    }
}
