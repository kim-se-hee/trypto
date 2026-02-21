package ksh.tryptobackend.trading.domain.model;

import ksh.tryptobackend.trading.domain.vo.Fee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Nested
    @DisplayName("수량 계산")
    class CalculateQuantityTest {

        @Test
        @DisplayName("정상 수량 계산 — 소수점 8자리까지 버림")
        void calculateQuantity_normal_flooredTo8Decimals() {
            BigDecimal amount = new BigDecimal("100000");
            BigDecimal price = new BigDecimal("100274000");

            BigDecimal quantity = Order.calculateQuantity(amount, price);

            assertThat(quantity).isEqualByComparingTo(new BigDecimal("0.00099726"));
        }

        @Test
        @DisplayName("수량 계산 경계값 — 나누어떨어지는 경우")
        void calculateQuantity_exactDivision_noRemainder() {
            BigDecimal amount = new BigDecimal("1000000");
            BigDecimal price = new BigDecimal("500000");

            BigDecimal quantity = Order.calculateQuantity(amount, price);

            assertThat(quantity).isEqualByComparingTo(new BigDecimal("2.00000000"));
        }

        @Test
        @DisplayName("수량 계산 경계값 — 소수점 9자리에서 버림 발생")
        void calculateQuantity_floorAt9thDecimal_truncated() {
            BigDecimal amount = new BigDecimal("1");
            BigDecimal price = new BigDecimal("3");

            BigDecimal quantity = Order.calculateQuantity(amount, price);

            // 1/3 = 0.333333333... → floor 8자리 = 0.33333333
            assertThat(quantity).isEqualByComparingTo(new BigDecimal("0.33333333"));
        }
    }

    @Nested
    @DisplayName("수수료 계산")
    class FeeCalculationTest {

        @Test
        @DisplayName("정상 수수료 계산")
        void calculate_normal_correctFee() {
            BigDecimal filledAmount = new BigDecimal("99726.44");
            BigDecimal feeRate = new BigDecimal("0.0005");

            Fee fee = Fee.calculate(filledAmount, feeRate);

            assertThat(fee.getAmount()).isEqualByComparingTo(new BigDecimal("49.86322000"));
        }

        @Test
        @DisplayName("수수료율 0% — 수수료 0")
        void calculate_zeroRate_zeroFee() {
            BigDecimal filledAmount = new BigDecimal("1000000");
            BigDecimal feeRate = BigDecimal.ZERO;

            Fee fee = Fee.calculate(filledAmount, feeRate);

            assertThat(fee.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}