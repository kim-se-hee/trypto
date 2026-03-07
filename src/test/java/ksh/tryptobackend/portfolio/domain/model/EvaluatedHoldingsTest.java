package ksh.tryptobackend.portfolio.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluatedHoldingsTest {

    @Test
    @DisplayName("여러 보유 종목의 평가금액을 합산한다")
    void totalEvaluatedAmount_multipleHoldings_sumsAll() {
        EvaluatedHolding btc = EvaluatedHolding.create(
            1L, new BigDecimal("90000000"), new BigDecimal("0.05"), new BigDecimal("95000000"));
        EvaluatedHolding eth = EvaluatedHolding.create(
            2L, new BigDecimal("5000000"), new BigDecimal("1"), new BigDecimal("5500000"));

        EvaluatedHoldings holdings = new EvaluatedHoldings(List.of(btc, eth));

        assertThat(holdings.totalEvaluatedAmount()).isEqualByComparingTo(new BigDecimal("10250000"));
    }

    @Test
    @DisplayName("빈 보유 목록의 평가금액은 0이다")
    void totalEvaluatedAmount_empty_returnsZero() {
        EvaluatedHoldings holdings = new EvaluatedHoldings(List.of());

        assertThat(holdings.totalEvaluatedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("스냅샷 상세 변환 시 비중과 수익률을 계산한다")
    void toSnapshotDetails_calculatesRatioAndProfitRate() {
        EvaluatedHolding btc = EvaluatedHolding.create(
            1L, new BigDecimal("90000000"), new BigDecimal("0.05"), new BigDecimal("95000000"));
        EvaluatedHolding eth = EvaluatedHolding.create(
            2L, new BigDecimal("5000000"), new BigDecimal("1"), new BigDecimal("5500000"));

        EvaluatedHoldings holdings = new EvaluatedHoldings(List.of(btc, eth));
        BigDecimal totalAsset = new BigDecimal("15250000");

        List<SnapshotDetail> details = holdings.toSnapshotDetails(totalAsset);

        assertThat(details).hasSize(2);

        SnapshotDetail btcDetail = details.get(0);
        assertThat(btcDetail.getCoinId()).isEqualTo(1L);
        assertThat(btcDetail.getProfitRate()).isEqualByComparingTo(new BigDecimal("5.5600"));
        assertThat(btcDetail.getAssetRatio()).isEqualByComparingTo(new BigDecimal("31.1500"));

        SnapshotDetail ethDetail = details.get(1);
        assertThat(ethDetail.getCoinId()).isEqualTo(2L);
        assertThat(ethDetail.getProfitRate()).isEqualByComparingTo(new BigDecimal("10.0000"));
        assertThat(ethDetail.getAssetRatio()).isEqualByComparingTo(new BigDecimal("36.0700"));
    }
}
