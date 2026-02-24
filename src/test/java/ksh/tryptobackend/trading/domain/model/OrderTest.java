package ksh.tryptobackend.trading.domain.model;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.trading.domain.vo.Fee;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Nested
    @DisplayName("수량 계산")
    class CalculateQuantityTest {

        @Test
        @DisplayName("정상 수량 계산 — 소수점 8자리까지 버림")
        void calculateQuantity_normal_flooredTo8Decimals() {
            BigDecimal amount = new BigDecimal("100000");
            BigDecimal price = new BigDecimal("100274000");

            Quantity quantity = Quantity.fromDivision(amount, price);

            assertThat(quantity.value()).isEqualByComparingTo(new BigDecimal("0.00099726"));
        }

        @Test
        @DisplayName("수량 계산 경계값 — 나누어떨어지는 경우")
        void calculateQuantity_exactDivision_noRemainder() {
            BigDecimal amount = new BigDecimal("1000000");
            BigDecimal price = new BigDecimal("500000");

            Quantity quantity = Quantity.fromDivision(amount, price);

            assertThat(quantity.value()).isEqualByComparingTo(new BigDecimal("2.00000000"));
        }

        @Test
        @DisplayName("수량 계산 경계값 — 소수점 9자리에서 버림 발생")
        void calculateQuantity_floorAt9thDecimal_truncated() {
            BigDecimal amount = new BigDecimal("1");
            BigDecimal price = new BigDecimal("3");

            Quantity quantity = Quantity.fromDivision(amount, price);

            // 1/3 = 0.333333333... → floor 8자리 = 0.33333333
            assertThat(quantity.value()).isEqualByComparingTo(new BigDecimal("0.33333333"));
        }
    }

    @Nested
    @DisplayName("주문 금액(orderAmount) 계산")
    class AmountCalculationTest {

        @Test
        @DisplayName("시장가 매수 — orderAmount는 체결 수량 × 현재가")
        void createMarketBuyOrder_orderAmount_equalsQuantityTimesPrice() {
            BigDecimal orderAmount = new BigDecimal("100000");
            BigDecimal currentPrice = new BigDecimal("100274000");

            Order order = Order.createMarketBuyOrder(
                UUID.randomUUID(), 1L, 1L,
                orderAmount, currentPrice, new BigDecimal("0.0005"), "KRW", LocalDateTime.now());

            BigDecimal expectedAmount = order.getQuantity().value().multiply(currentPrice);
            assertThat(order.getOrderAmount()).isEqualByComparingTo(expectedAmount);
        }

        @Test
        @DisplayName("시장가 매도 — orderAmount는 매도 수량 × 현재가")
        void createMarketSellOrder_orderAmount_equalsQuantityTimesPrice() {
            BigDecimal sellQuantity = new BigDecimal("0.5");
            BigDecimal currentPrice = new BigDecimal("100274000");

            Order order = Order.createMarketSellOrder(
                UUID.randomUUID(), 1L, 1L,
                sellQuantity, currentPrice, new BigDecimal("0.0005"), LocalDateTime.now());

            BigDecimal expectedAmount = sellQuantity.multiply(currentPrice);
            assertThat(order.getOrderAmount()).isEqualByComparingTo(expectedAmount);
        }

        @Test
        @DisplayName("지정가 매수 — orderAmount는 체결 수량 × 지정가")
        void createLimitBuyOrder_orderAmount_equalsQuantityTimesLimitPrice() {
            BigDecimal orderAmount = new BigDecimal("500000");
            BigDecimal limitPrice = new BigDecimal("100000000");

            Order order = Order.createLimitBuyOrder(
                UUID.randomUUID(), 1L, 1L,
                orderAmount, limitPrice, new BigDecimal("0.0005"), "KRW", LocalDateTime.now());

            BigDecimal expectedAmount = order.getQuantity().value().multiply(limitPrice);
            assertThat(order.getOrderAmount()).isEqualByComparingTo(expectedAmount);
        }

        @Test
        @DisplayName("지정가 매도 — orderAmount는 매도 수량 × 지정가")
        void createLimitSellOrder_orderAmount_equalsQuantityTimesLimitPrice() {
            BigDecimal sellQuantity = new BigDecimal("0.001");
            BigDecimal limitPrice = new BigDecimal("110000000");

            Order order = Order.createLimitSellOrder(
                UUID.randomUUID(), 1L, 1L,
                sellQuantity, limitPrice, new BigDecimal("0.0005"), LocalDateTime.now());

            BigDecimal expectedAmount = sellQuantity.multiply(limitPrice);
            assertThat(order.getOrderAmount()).isEqualByComparingTo(expectedAmount);
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

            assertThat(fee.amount()).isEqualByComparingTo(new BigDecimal("49.86322000"));
        }

        @Test
        @DisplayName("수수료율 0% — 수수료 0")
        void calculate_zeroRate_zeroFee() {
            BigDecimal filledAmount = new BigDecimal("1000000");
            BigDecimal feeRate = BigDecimal.ZERO;

            Fee fee = Fee.calculate(filledAmount, feeRate);

            assertThat(fee.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("주문 취소")
    class CancelTest {

        @Test
        @DisplayName("PENDING 주문 취소 성공")
        void cancel_pendingOrder_cancelledSuccessfully() {
            Order order = Order.createLimitBuyOrder(
                UUID.randomUUID(), 1L, 1L,
                new BigDecimal("500000"), new BigDecimal("100000000"),
                new BigDecimal("0.0005"), "KRW", LocalDateTime.now());

            order.cancel();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("FILLED 주문 취소 시도 — 예외 발생")
        void cancel_filledOrder_throwsException() {
            Order order = Order.createMarketBuyOrder(
                UUID.randomUUID(), 1L, 1L,
                new BigDecimal("100000"), new BigDecimal("100274000"),
                new BigDecimal("0.0005"), "KRW", LocalDateTime.now());

            assertThatThrownBy(order::cancel)
                .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("이미 취소된 주문 재취소 — 멱등성 보장")
        void cancel_alreadyCancelled_idempotent() {
            Order order = Order.createLimitBuyOrder(
                UUID.randomUUID(), 1L, 1L,
                new BigDecimal("500000"), new BigDecimal("100000000"),
                new BigDecimal("0.0005"), "KRW", LocalDateTime.now());

            order.cancel();
            order.cancel();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
    }
}
