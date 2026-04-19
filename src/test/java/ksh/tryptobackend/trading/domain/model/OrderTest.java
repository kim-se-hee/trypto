package ksh.tryptobackend.trading.domain.model;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.domain.vo.Fee;
import ksh.tryptobackend.trading.domain.vo.OrderAmountPolicy;
import ksh.tryptobackend.trading.domain.vo.OrderMode;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.trading.domain.vo.TradingContext;
import ksh.tryptobackend.trading.domain.vo.TradingVenue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final TradingVenue DOMESTIC_VENUE = new TradingVenue(
        new BigDecimal("0.0005"), 1L, OrderAmountPolicy.DOMESTIC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 17, 12, 0, 0);

    private static PlaceOrderCommand cmd(Side side, OrderType orderType,
                                          BigDecimal amount, BigDecimal price) {
        return new PlaceOrderCommand(UUID.randomUUID().toString(), 1L, 1L,
            side, orderType, price, amount);
    }

    private static TradingContext ctx(BigDecimal currentPrice) {
        return new TradingContext(1L, 100L, DOMESTIC_VENUE, OrderMode.MARKET_BUY, currentPrice, NOW);
    }

    private static TradingContext ctx(OrderMode mode, BigDecimal currentPrice) {
        return new TradingContext(1L, 100L, DOMESTIC_VENUE, mode, currentPrice, NOW);
    }

    @Nested
    @DisplayName("수량 계산")
    class CalculateQuantityTest {

        @Test
        @DisplayName("정상 수량 계산 — 소수점 8자리까지 버림")
        void calculateQuantity_normal_flooredTo8Decimals() {
            BigDecimal amount = new BigDecimal("100000");
            BigDecimal price = new BigDecimal("100274000");

            Quantity quantity = Quantity.fromAmountAndPrice(amount, price);

            assertThat(quantity.value()).isEqualByComparingTo(new BigDecimal("0.00099726"));
        }

        @Test
        @DisplayName("수량 계산 경계값 — 나누어떨어지는 경우")
        void calculateQuantity_exactDivision_noRemainder() {
            BigDecimal amount = new BigDecimal("1000000");
            BigDecimal price = new BigDecimal("500000");

            Quantity quantity = Quantity.fromAmountAndPrice(amount, price);

            assertThat(quantity.value()).isEqualByComparingTo(new BigDecimal("2.00000000"));
        }

        @Test
        @DisplayName("수량 계산 경계값 — 소수점 9자리에서 버림 발생")
        void calculateQuantity_floorAt9thDecimal_truncated() {
            BigDecimal amount = new BigDecimal("1");
            BigDecimal price = new BigDecimal("3");

            Quantity quantity = Quantity.fromAmountAndPrice(amount, price);

            // 1/3 = 0.333333333... → floor 8자리 = 0.33333333
            assertThat(quantity.value()).isEqualByComparingTo(new BigDecimal("0.33333333"));
        }
    }

    @Nested
    @DisplayName("주문 금액(amount) 계산")
    class AmountCalculationTest {

        @Test
        @DisplayName("시장가 매수 — amount는 체결 수량 × 현재가")
        void createMarketBuyOrder_amount_equalsQuantityTimesPrice() {
            BigDecimal orderAmount = new BigDecimal("100000");
            BigDecimal currentPrice = new BigDecimal("100274000");

            Order order = Order.create(
                cmd(Side.BUY, OrderType.MARKET, orderAmount, null),
                ctx(currentPrice));

            BigDecimal expectedAmount = order.getQuantity().value().multiply(currentPrice);
            assertThat(order.getAmount()).isEqualByComparingTo(expectedAmount);
        }

        @Test
        @DisplayName("시장가 매도 — amount는 매도 수량 × 현재가")
        void createMarketSellOrder_amount_equalsQuantityTimesPrice() {
            BigDecimal sellQuantity = new BigDecimal("0.5");
            BigDecimal currentPrice = new BigDecimal("100274000");

            Order order = Order.create(
                cmd(Side.SELL, OrderType.MARKET, sellQuantity, null),
                ctx(currentPrice));

            BigDecimal expectedAmount = sellQuantity.multiply(currentPrice);
            assertThat(order.getAmount()).isEqualByComparingTo(expectedAmount);
        }

        @Test
        @DisplayName("지정가 매수 — amount는 체결 수량 × 지정가")
        void createLimitBuyOrder_amount_equalsQuantityTimesLimitPrice() {
            BigDecimal orderAmount = new BigDecimal("500000");
            BigDecimal limitPrice = new BigDecimal("100000000");

            Order order = Order.create(
                cmd(Side.BUY, OrderType.LIMIT, orderAmount, limitPrice),
                ctx(limitPrice));

            BigDecimal expectedAmount = order.getQuantity().value().multiply(limitPrice);
            assertThat(order.getAmount()).isEqualByComparingTo(expectedAmount);
        }

        @Test
        @DisplayName("지정가 매도 — amount는 매도 수량 × 지정가")
        void createLimitSellOrder_amount_equalsQuantityTimesLimitPrice() {
            BigDecimal sellQuantity = new BigDecimal("0.001");
            BigDecimal limitPrice = new BigDecimal("110000000");

            Order order = Order.create(
                cmd(Side.SELL, OrderType.LIMIT, sellQuantity, limitPrice),
                ctx(limitPrice));

            BigDecimal expectedAmount = sellQuantity.multiply(limitPrice);
            assertThat(order.getAmount()).isEqualByComparingTo(expectedAmount);
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
    @DisplayName("주문 체결")
    class FillTest {

        @Test
        @DisplayName("PENDING 주문 체결 성공 - 상태가 FILLED로 변경되고 filledAt이 설정된다")
        void fill_pendingOrder_filledSuccessfully() {
            Order order = Order.create(
                cmd(Side.BUY, OrderType.LIMIT, new BigDecimal("500000"), new BigDecimal("100000000")),
                ctx(new BigDecimal("100000000")));
            LocalDateTime fillTime = LocalDateTime.of(2026, 3, 17, 12, 0, 0);

            order.fill(fillTime);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
            assertThat(order.getFilledAt()).isEqualTo(fillTime);
        }

        @Test
        @DisplayName("FILLED 주문에 fill 시도 - 예외 발생")
        void fill_filledOrder_throwsException() {
            Order order = Order.create(
                cmd(Side.BUY, OrderType.MARKET, new BigDecimal("100000"), null),
                ctx(new BigDecimal("100274000")));

            assertThatThrownBy(() -> order.fill(LocalDateTime.now()))
                .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("CANCELLED 주문에 fill 시도 - 예외 발생")
        void fill_cancelledOrder_throwsException() {
            Order order = Order.create(
                cmd(Side.BUY, OrderType.LIMIT, new BigDecimal("500000"), new BigDecimal("100000000")),
                ctx(new BigDecimal("100000000")));
            order.cancel();

            assertThatThrownBy(() -> order.fill(LocalDateTime.now()))
                .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("isPending 판별")
    class IsPendingTest {

        @Test
        @DisplayName("지정가 주문 생성 직후 - isPending이 true")
        void isPending_limitOrder_true() {
            Order order = Order.create(
                cmd(Side.BUY, OrderType.LIMIT, new BigDecimal("500000"), new BigDecimal("100000000")),
                ctx(new BigDecimal("100000000")));

            assertThat(order.isPending()).isTrue();
        }

        @Test
        @DisplayName("시장가 주문 생성 직후 - isPending이 false")
        void isPending_marketOrder_false() {
            Order order = Order.create(
                cmd(Side.BUY, OrderType.MARKET, new BigDecimal("100000"), null),
                ctx(new BigDecimal("100274000")));

            assertThat(order.isPending()).isFalse();
        }

        @Test
        @DisplayName("체결된 주문 - isPending이 false")
        void isPending_filledOrder_false() {
            Order order = Order.create(
                cmd(Side.BUY, OrderType.LIMIT, new BigDecimal("500000"), new BigDecimal("100000000")),
                ctx(new BigDecimal("100000000")));
            order.fill(LocalDateTime.now());

            assertThat(order.isPending()).isFalse();
        }
    }

    @Nested
    @DisplayName("주문 취소")
    class CancelTest {

        @Test
        @DisplayName("PENDING 주문 취소 성공")
        void cancel_pendingOrder_cancelledSuccessfully() {
            Order order = Order.create(
                cmd(Side.BUY, OrderType.LIMIT, new BigDecimal("500000"), new BigDecimal("100000000")),
                ctx(new BigDecimal("100000000")));

            order.cancel();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("FILLED 주문 취소 시도 — 예외 발생")
        void cancel_filledOrder_throwsException() {
            Order order = Order.create(
                cmd(Side.BUY, OrderType.MARKET, new BigDecimal("100000"), null),
                ctx(new BigDecimal("100274000")));

            assertThatThrownBy(order::cancel)
                .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("이미 취소된 주문 재취소 — 멱등성 보장")
        void cancel_alreadyCancelled_idempotent() {
            Order order = Order.create(
                cmd(Side.BUY, OrderType.LIMIT, new BigDecimal("500000"), new BigDecimal("100000000")),
                ctx(new BigDecimal("100000000")));

            order.cancel();
            order.cancel();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
    }
}
