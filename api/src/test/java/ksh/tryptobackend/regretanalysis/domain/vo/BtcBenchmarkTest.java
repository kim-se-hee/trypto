package ksh.tryptobackend.regretanalysis.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BtcBenchmarkTest {

    private static final LocalDate DAY_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate DAY_2 = LocalDate.of(2025, 1, 2);
    private static final LocalDate DAY_3 = LocalDate.of(2025, 1, 3);

    private static BtcDailyPrices pricesOf(BtcDailyPrice... prices) {
        return BtcDailyPrices.of(List.of(prices));
    }

    private static BtcDailyPrice priceOn(LocalDate date, String closePrice) {
        return new BtcDailyPrice(date, new BigDecimal(closePrice));
    }

    private static CapitalInflows seedOnly(String seedMoney) {
        return CapitalInflows.of(new BigDecimal(seedMoney), List.of(), DAY_1);
    }

    @Nested
    @DisplayName("BTC 벤치마크 계산")
    class CalculateTest {

        @Test
        @DisplayName("시드머니로 BTC를 매수한 뒤 일별 가치를 계산한다")
        void calculate_normalCase_dailyValuesComputed() {
            // Given
            BtcDailyPrices prices =
                    pricesOf(priceOn(DAY_1, "50000000"), priceOn(DAY_2, "55000000"), priceOn(DAY_3, "45000000"));
            List<LocalDate> dates = List.of(DAY_1, DAY_2, DAY_3);

            // When
            BtcBenchmark benchmark = BtcBenchmark.calculate(seedOnly("1000000"), prices, dates);

            // Then
            assertThat(benchmark.getAssetValueAt(DAY_1)).isEqualByComparingTo(new BigDecimal("1000000"));
            assertThat(benchmark.getAssetValueAt(DAY_2)).isEqualByComparingTo(new BigDecimal("1100000"));
            assertThat(benchmark.getAssetValueAt(DAY_3)).isEqualByComparingTo(new BigDecimal("900000"));
        }

        @Test
        @DisplayName("긴급 충전은 충전일 종가로 추가 매수한다")
        void calculate_emergencyCharge_buysMoreAtChargedDatePrice() {
            // Given
            BtcDailyPrices prices =
                    pricesOf(priceOn(DAY_1, "50000000"), priceOn(DAY_2, "40000000"), priceOn(DAY_3, "50000000"));
            CapitalInflows inflows = CapitalInflows.of(
                    new BigDecimal("1000000"), List.of(new EmergencyCharge(DAY_2, new BigDecimal("400000"))), DAY_1);

            // When
            BtcBenchmark benchmark = BtcBenchmark.calculate(inflows, prices, List.of(DAY_1, DAY_2, DAY_3));

            // Then
            assertThat(benchmark.getAssetValueAt(DAY_1)).isEqualByComparingTo(new BigDecimal("1000000"));
            assertThat(benchmark.getAssetValueAt(DAY_2)).isEqualByComparingTo(new BigDecimal("1200000"));
            assertThat(benchmark.getAssetValueAt(DAY_3)).isEqualByComparingTo(new BigDecimal("1500000"));
        }

        @Test
        @DisplayName("첫날보다 앞선 충전은 시드머니와 함께 첫날에 매수한다")
        void calculate_chargeBeforeFirstDate_buysOnFirstDate() {
            // Given
            BtcDailyPrices prices = pricesOf(priceOn(DAY_1, "50000000"));
            CapitalInflows inflows = CapitalInflows.of(
                    new BigDecimal("1000000"),
                    List.of(new EmergencyCharge(DAY_1.minusDays(3), new BigDecimal("500000"))),
                    DAY_1);

            // When
            BtcBenchmark benchmark = BtcBenchmark.calculate(inflows, prices, List.of(DAY_1));

            // Then
            assertThat(benchmark.getAssetValueAt(DAY_1)).isEqualByComparingTo(new BigDecimal("1500000"));
        }

        @Test
        @DisplayName("시작일 BTC 가격이 0이면 자산은 0이다")
        void calculate_zeroPriceAtStart_zeroAsset() {
            // Given
            BtcDailyPrices prices = pricesOf(priceOn(DAY_1, "0"));

            // When
            BtcBenchmark benchmark = BtcBenchmark.calculate(seedOnly("1000000"), prices, List.of(DAY_1));

            // Then
            assertThat(benchmark.getAssetValueAt(DAY_1)).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("시작일 BTC 가격이 없으면 자산은 0이다")
        void calculate_noPriceAtStart_zeroAsset() {
            // Given
            BtcDailyPrices prices = pricesOf();

            // When
            BtcBenchmark benchmark = BtcBenchmark.calculate(seedOnly("1000000"), prices, List.of(DAY_1));

            // Then
            assertThat(benchmark.getAssetValueAt(DAY_1)).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("특정 날짜에 BTC 가격이 없으면 해당 날짜 자산은 0이다")
        void calculate_missingPriceOnDate_zeroForThatDate() {
            // Given
            BtcDailyPrices prices = pricesOf(priceOn(DAY_1, "50000000"), priceOn(DAY_3, "60000000"));
            List<LocalDate> dates = List.of(DAY_1, DAY_2, DAY_3);

            // When
            BtcBenchmark benchmark = BtcBenchmark.calculate(seedOnly("1000000"), prices, dates);

            // Then
            assertThat(benchmark.getAssetValueAt(DAY_1)).isEqualByComparingTo(new BigDecimal("1000000"));
            assertThat(benchmark.getAssetValueAt(DAY_2)).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(benchmark.getAssetValueAt(DAY_3)).isEqualByComparingTo(new BigDecimal("1200000"));
        }

        @Test
        @DisplayName("소수점 나눗셈 정밀도가 유지된다")
        void calculate_fractionalDivision_precisionMaintained() {
            // Given
            BigDecimal seedMoney = new BigDecimal("1000000");
            BtcDailyPrices prices = pricesOf(priceOn(DAY_1, "30000000"), priceOn(DAY_2, "30000000"));

            // When
            BtcBenchmark benchmark = BtcBenchmark.calculate(seedOnly("1000000"), prices, List.of(DAY_1, DAY_2));

            // Then
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
            BtcDailyPrices prices = pricesOf(priceOn(DAY_1, "50000000"));
            BtcBenchmark benchmark = BtcBenchmark.calculate(seedOnly("1000000"), prices, List.of(DAY_1));

            // When
            BigDecimal value = benchmark.getAssetValueAt(DAY_3);

            // Then
            assertThat(value).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
