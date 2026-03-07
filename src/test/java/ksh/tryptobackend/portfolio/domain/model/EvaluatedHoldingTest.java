package ksh.tryptobackend.portfolio.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluatedHoldingTest {

    @Test
    @DisplayName("현재가 x 수량으로 평가금액을 계산한다")
    void create_normal_calculatesEvaluatedAmount() {
        EvaluatedHolding holding = EvaluatedHolding.create(
            1L,
            new BigDecimal("90000000"),
            new BigDecimal("0.05"),
            new BigDecimal("95000000"));

        assertThat(holding.getEvaluatedAmount()).isEqualByComparingTo(new BigDecimal("4750000"));
    }

    @Test
    @DisplayName("toSnapshotDetail 변환 시 수익률과 비중을 계산한다")
    void toSnapshotDetail_calculatesProfitRateAndRatio() {
        EvaluatedHolding holding = EvaluatedHolding.create(
            1L,
            new BigDecimal("90000000"),
            new BigDecimal("0.05"),
            new BigDecimal("95000000"));

        BigDecimal totalAsset = new BigDecimal("9750000");
        SnapshotDetail detail = holding.toSnapshotDetail(totalAsset);

        assertThat(detail.getCoinId()).isEqualTo(1L);
        assertThat(detail.getQuantity()).isEqualByComparingTo(new BigDecimal("0.05"));
        assertThat(detail.getAvgBuyPrice()).isEqualByComparingTo(new BigDecimal("90000000"));
        assertThat(detail.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("95000000"));
        assertThat(detail.getProfitRate()).isEqualByComparingTo(new BigDecimal("5.5600"));
        assertThat(detail.getAssetRatio()).isEqualByComparingTo(new BigDecimal("48.7200"));
    }
}
