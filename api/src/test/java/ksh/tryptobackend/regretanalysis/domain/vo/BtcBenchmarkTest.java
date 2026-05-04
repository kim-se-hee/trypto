package ksh.tryptobackend.regretanalysis.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BtcBenchmarkTest {

    private static final LocalDate DAY_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate DAY_2 = LocalDate.of(2025, 1, 2);
    private static final LocalDate DAY_3 = LocalDate.of(2025, 1, 3);

    @Nested
    @DisplayName("BTC 벤치마크 계산")
    class CalculateTest {

        @Test
        @DisplayName("시드머니로 BTC를 매수한 뒤 일별 가치를 계산한다")
        void calculate_normalCase_dailyValuesComputed() {
            // Given
            BigDecimal seedMoney = new BigDecimal("1000000");
            BigDecimal btcPriceDay1 = new BigDecimal("50000000");
            BigDecimal btcPriceDay2 = new BigDecimal("55000000");
            BigDecimal btcPriceDay3 = new BigDecimal("45000000");
            Map<LocalDate, BigDecimal> priceMap =
                    Map.of(
                            DAY_1, btcPriceDay1,
                            DAY_2, btcPriceDay2,
                            DAY_3, btcPriceDay3);
            List<LocalDate> dates = List.of(DAY_1, DAY_2, DAY_3);

            // When
            BtcBenchmark benchmark = BtcBenchmark.calculate(seedMoney, priceMap, dates, DAY_1);

            // Then
            // btcQuantity = 1000000 / 50000000 = 0.02
            // DAY_1: 0.02 * 50000000 = 1000000
            // DAY_2: 0.02 * 55000000 = 1100000
            // DAY_3: 0.02 * 45000000 = 900000
            assertThat(benchmark.getAssetValueAt(DAY_1))
                    .isEqualByComparingTo(new BigDecimal("1000000"));
            assertThat(benchmark.getAssetValueAt(DAY_2))
                    .isEqualByComparingTo(new BigDecimal("1100000"));
            assertThat(benchmark.getAssetValueAt(DAY_3))
                    .isEqualByComparingTo(new BigDecimal("900000"));
        }

        @Test
        @DisplayName("시작일 BTC 가격이 0이면 빈 벤치마크를 반환한다")
        void calculate_zeroPriceAtStart_emptyBenchmark() {
            // Given
            BigDecimal seedMoney = new BigDecimal("1000000");
            Map<LocalDate, BigDecimal> priceMap = Map.of(DAY_1, BigDecimal.ZERO);
            List<LocalDate> dates = List.of(DAY_1);

            // When
            BtcBenchmark benchmark = BtcBenchmark.calculate(seedMoney, priceMap, dates, DAY_1);

            // Then
            assertThat(benchmark.getAssetValueAt(DAY_1)).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("시작일 BTC 가격이 없으면 빈 벤치마크를 반환한다")
        void calculate_nullPriceAtStart_emptyBenchmark() {
            // Given
            BigDecimal seedMoney = new BigDecimal("1000000");
            Map<LocalDate, BigDecimal> priceMap = Map.of();
            List<LocalDate> dates = List.of(DAY_1);

            // When
            BtcBenchmark benchmark = BtcBenchmark.calculate(seedMoney, priceMap, dates, DAY_1);

            // Then
            assertThat(benchmark.getAssetValueAt(DAY_1)).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("특정 날짜에 BTC 가격이 없으면 해당 날짜 자산은 0이다")
        void calculate_missingPriceOnDate_zeroForThatDate() {
            // Given
            BigDecimal seedMoney = new BigDecimal("1000000");
            Map<LocalDate, BigDecimal> priceMap =
                    Map.of(
                            DAY_1, new BigDecimal("50000000"),
                            DAY_3, new BigDecimal("60000000"));
            List<LocalDate> dates = List.of(DAY_1, DAY_2, DAY_3);

            // When
            BtcBenchmark benchmark = BtcBenchmark.calculate(seedMoney, priceMap, dates, DAY_1);

            // Then
            assertThat(benchmark.getAssetValueAt(DAY_1))
                    .isEqualByComparingTo(new BigDecimal("1000000"));
            assertThat(benchmark.getAssetValueAt(DAY_2)).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(benchmark.getAssetValueAt(DAY_3))
                    .isEqualByComparingTo(new BigDecimal("1200000"));
        }

        @Test
        @DisplayName("소수점 나눗셈 정밀도가 유지된다")
        void calculate_fractionalDivision_precisionMaintained() {
            // Given
            BigDecimal seedMoney = new BigDecimal("1000000");
            BigDecimal btcPriceDay1 = new BigDecimal("30000000");
            Map<LocalDate, BigDecimal> priceMap =
                    Map.of(DAY_1, btcPriceDay1, DAY_2, new BigDecimal("30000000"));
            List<LocalDate> dates = List.of(DAY_1, DAY_2);

            // When
            BtcBenchmark benchmark = BtcBenchmark.calculate(seedMoney, priceMap, dates, DAY_1);

            // Then
            // btcQuantity = 1000000 / 30000000 = 0.03333333 (8자리)
            // DAY_1: 0.03333333 * 30000000 ≈ 999999.9 (근사치이지만 원래 시드머니에 가까워야 함)
            // DAY_2: 동일
            BigDecimal day1Value = benchmark.getAssetValueAt(DAY_1);
            assertThat(day1Value).isNotNull();
            assertThat(day1Value.subtract(seedMoney).abs()).isLessThan(new BigDecimal("10"));
        }
    }

    @Nested
    @DisplayName("일별 자산 조회")
    class GetAssetValueAtTest {

        @Test
        @DisplayName("존재하지 않는 날짜를 조회하면 0을 반환한다")
        void getAssetValueAt_nonExistentDate_returnsZero() {
            // Given
            BigDecimal seedMoney = new BigDecimal("1000000");
            Map<LocalDate, BigDecimal> priceMap = Map.of(DAY_1, new BigDecimal("50000000"));
            BtcBenchmark benchmark =
                    BtcBenchmark.calculate(seedMoney, priceMap, List.of(DAY_1), DAY_1);

            // When
            BigDecimal value = benchmark.getAssetValueAt(DAY_3);

            // Then
            assertThat(value).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
