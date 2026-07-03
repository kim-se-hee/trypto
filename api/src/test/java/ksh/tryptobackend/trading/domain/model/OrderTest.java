package ksh.tryptobackend.trading.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.domain.event.OrderCanceledEvent;
import ksh.tryptobackend.trading.domain.event.OrderFilledEvent;
import ksh.tryptobackend.trading.domain.event.OrderPlacedEvent;
import ksh.tryptobackend.trading.domain.vo.ExchangeInfo;
import ksh.tryptobackend.trading.domain.vo.Fee;
import ksh.tryptobackend.trading.domain.vo.MarketInfo;
import ksh.tryptobackend.trading.domain.vo.Money;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Price;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.trading.domain.vo.TradingPair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final ExchangeInfo DOMESTIC_EXCHANGE_INFO =
            new ExchangeInfo(
                    new BigDecimal("0.0005"), new BigDecimal("5000"), new BigDecimal("1000000000"));
    private static final TradingPair PAIR = new TradingPair(100L, 1L);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 17, 12, 0, 0);

    private static PlaceOrderCommand cmd(
            Side side, OrderType orderType, BigDecimal amount, BigDecimal price) {
        return new PlaceOrderCommand(
                UUID.randomUUID().toString(), 1L, 1L, side, orderType, price, amount);
    }

    private static MarketInfo ctx(BigDecimal currentPrice) {
        return new MarketInfo(PAIR, DOMESTIC_EXCHANGE_INFO, Price.of(currentPrice));
    }

    @Nested
    @DisplayName("수량 계산")
    class CalculateQuantityTest {

        @Test
        @DisplayName("정상 수량 계산 — 소수점 8자리까지 버림")
        void calculateQuantity_normal_flooredTo8Decimals() {
            BigDecimal amount = new BigDecimal("100000");
            BigDecimal price = new BigDecimal("100274000");

            Quantity quantity = Quantity.from(amount, Price.of(price));

            assertThat(quantity.value()).isEqualByComparingTo(new BigDecimal("0.00099726"));
        }

        @Test
        @DisplayName("수량 계산 경계값 — 나누어떨어지는 경우")
        void calculateQuantity_exactDivision_noRemainder() {
            BigDecimal amount = new BigDecimal("1000000");
            BigDecimal price = new BigDecimal("500000");

            Quantity quantity = Quantity.from(amount, Price.of(price));

            assertThat(quantity.value()).isEqualByComparingTo(new BigDecimal("2.00000000"));
        }

        @Test
        @DisplayName("수량 계산 경계값 — 소수점 9자리에서 버림 발생")
        void calculateQuantity_floorAt9thDecimal_truncated() {
            BigDecimal amount = new BigDecimal("1");
            BigDecimal price = new BigDecimal("3");

            Quantity quantity = Quantity.from(amount, Price.of(price));

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

            Order order =
                    Order.create(
                            cmd(Side.BUY, OrderType.MARKET, orderAmount, null),
                            ctx(currentPrice),
                            NOW);

            BigDecimal expectedAmount = order.getQuantity().value().multiply(currentPrice);
            assertThat(order.getFilledAmount().value()).isEqualByComparingTo(expectedAmount);
        }

        @Test
        @DisplayName("시장가 매도 — amount는 매도 수량 × 현재가")
        void createMarketSellOrder_amount_equalsQuantityTimesPrice() {
            BigDecimal sellQuantity = new BigDecimal("0.5");
            BigDecimal currentPrice = new BigDecimal("100274000");

            Order order =
                    Order.create(
                            cmd(Side.SELL, OrderType.MARKET, sellQuantity, null),
                            ctx(currentPrice),
                            NOW);

            BigDecimal expectedAmount = sellQuantity.multiply(currentPrice);
            assertThat(order.getFilledAmount().value()).isEqualByComparingTo(expectedAmount);
        }

        @Test
        @DisplayName("지정가 매수 — 미체결이면 amount·fee는 null, 체결 후 체결가 기준으로 확정")
        void createLimitBuyOrder_amountAndFee_realizedOnFill() {
            BigDecimal orderAmount = new BigDecimal("500000");
            BigDecimal limitPrice = new BigDecimal("100000000");
            BigDecimal executionPrice = new BigDecimal("99000000");

            Order order =
                    Order.create(
                            cmd(Side.BUY, OrderType.LIMIT, orderAmount, limitPrice),
                            ctx(limitPrice),
                            NOW);

            assertThat(order.getFilledAmount()).isNull();
            assertThat(order.getFee()).isNull();

            order.fill(executionPrice, NOW);

            BigDecimal expectedAmount = order.getQuantity().value().multiply(executionPrice);
            assertThat(order.getFilledAmount().value()).isEqualByComparingTo(expectedAmount);
            assertThat(order.getFee().amount().value())
                    .isEqualByComparingTo(expectedAmount.multiply(new BigDecimal("0.0005")));
        }

        @Test
        @DisplayName("지정가 매도 — 미체결이면 amount는 null, 체결 후 체결가 × 수량")
        void createLimitSellOrder_amount_realizedOnFill() {
            BigDecimal sellQuantity = new BigDecimal("0.001");
            BigDecimal limitPrice = new BigDecimal("110000000");
            BigDecimal executionPrice = new BigDecimal("111000000");

            Order order =
                    Order.create(
                            cmd(Side.SELL, OrderType.LIMIT, sellQuantity, limitPrice),
                            ctx(limitPrice),
                            NOW);

            assertThat(order.getFilledAmount()).isNull();

            order.fill(executionPrice, NOW);

            BigDecimal expectedAmount = sellQuantity.multiply(executionPrice);
            assertThat(order.getFilledAmount().value()).isEqualByComparingTo(expectedAmount);
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

            Fee fee = Fee.calculate(Money.of(filledAmount), feeRate);

            assertThat(fee.amount().value()).isEqualByComparingTo(new BigDecimal("49.86322000"));
        }

        @Test
        @DisplayName("수수료율 0% — 수수료 0")
        void calculate_zeroRate_zeroFee() {
            BigDecimal filledAmount = new BigDecimal("1000000");
            BigDecimal feeRate = BigDecimal.ZERO;

            Fee fee = Fee.calculate(Money.of(filledAmount), feeRate);

            assertThat(fee.amount().value()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("주문 체결")
    class FillTest {

        @Test
        @DisplayName("PENDING 주문 체결 성공 - 상태가 FILLED로 변경되고 filledAt이 설정된다")
        void fill_pendingOrder_filledSuccessfully() {
            Order order =
                    Order.create(
                            cmd(
                                    Side.BUY,
                                    OrderType.LIMIT,
                                    new BigDecimal("500000"),
                                    new BigDecimal("100000000")),
                            ctx(new BigDecimal("100000000")),
                            NOW);
            LocalDateTime fillTime = LocalDateTime.of(2026, 3, 17, 12, 0, 0);

            order.fill(new BigDecimal("100000000"), fillTime);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
            assertThat(order.getFilledAt()).isEqualTo(fillTime);
        }

        @Test
        @DisplayName("FILLED 주문에 fill 시도 - 예외 발생")
        void fill_filledOrder_throwsException() {
            Order order =
                    Order.create(
                            cmd(Side.BUY, OrderType.MARKET, new BigDecimal("100000"), null),
                            ctx(new BigDecimal("100274000")),
                            NOW);

            assertThatThrownBy(() -> order.fill(new BigDecimal("100274000"), LocalDateTime.now()))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("주문 취소")
    class CancelTest {

        @Test
        @DisplayName("PENDING 지정가 주문 취소 - 상태가 CANCELLED로 바뀌고 취소 이벤트가 등록된다")
        void cancel_pendingOrder_cancelled() {
            Order order =
                    Order.create(
                            cmd(
                                    Side.BUY,
                                    OrderType.LIMIT,
                                    new BigDecimal("500000"),
                                    new BigDecimal("100000000")),
                            ctx(new BigDecimal("100000000")),
                            NOW);
            order.pullDomainEvents();

            order.cancel();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.pullDomainEvents())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(OrderCanceledEvent.class);
        }

        @Test
        @DisplayName("체결된 주문 취소 시도 - 예외 발생")
        void cancel_filledOrder_throwsException() {
            Order order =
                    Order.create(
                            cmd(Side.BUY, OrderType.MARKET, new BigDecimal("100000"), null),
                            ctx(new BigDecimal("100274000")),
                            NOW);

            assertThatThrownBy(order::cancel).isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("도메인 이벤트 등록")
    class DomainEventTest {

        @Test
        @DisplayName("시장가 주문 생성 - OrderPlacedEvent와 OrderFilledEvent가 등록된다")
        void create_marketOrder_registersPlacedAndFilledEvents() {
            Order order =
                    Order.create(
                            cmd(Side.BUY, OrderType.MARKET, new BigDecimal("100000"), null),
                            ctx(new BigDecimal("100274000")),
                            NOW);

            assertThat(order.pullDomainEvents())
                    .hasSize(2)
                    .hasExactlyElementsOfTypes(OrderPlacedEvent.class, OrderFilledEvent.class);
        }

        @Test
        @DisplayName("지정가 주문 생성 - OrderPlacedEvent만 등록된다")
        void create_limitOrder_registersPlacedEventOnly() {
            Order order =
                    Order.create(
                            cmd(
                                    Side.BUY,
                                    OrderType.LIMIT,
                                    new BigDecimal("500000"),
                                    new BigDecimal("100000000")),
                            ctx(new BigDecimal("100000000")),
                            NOW);

            assertThat(order.pullDomainEvents())
                    .hasSize(1)
                    .hasExactlyElementsOfTypes(OrderPlacedEvent.class);
        }

        @Test
        @DisplayName("이벤트는 한 번 꺼내면 비워진다")
        void pullDomainEvents_clearsEvents() {
            Order order =
                    Order.create(
                            cmd(Side.BUY, OrderType.MARKET, new BigDecimal("100000"), null),
                            ctx(new BigDecimal("100274000")),
                            NOW);

            order.pullDomainEvents();

            assertThat(order.pullDomainEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("isPending 판별")
    class IsPendingTest {

        @Test
        @DisplayName("지정가 주문 생성 직후 - isPending이 true")
        void isPending_limitOrder_true() {
            Order order =
                    Order.create(
                            cmd(
                                    Side.BUY,
                                    OrderType.LIMIT,
                                    new BigDecimal("500000"),
                                    new BigDecimal("100000000")),
                            ctx(new BigDecimal("100000000")),
                            NOW);

            assertThat(order.isPending()).isTrue();
        }

        @Test
        @DisplayName("시장가 주문 생성 직후 - isPending이 false")
        void isPending_marketOrder_false() {
            Order order =
                    Order.create(
                            cmd(Side.BUY, OrderType.MARKET, new BigDecimal("100000"), null),
                            ctx(new BigDecimal("100274000")),
                            NOW);

            assertThat(order.isPending()).isFalse();
        }

        @Test
        @DisplayName("체결된 주문 - isPending이 false")
        void isPending_filledOrder_false() {
            Order order =
                    Order.create(
                            cmd(
                                    Side.BUY,
                                    OrderType.LIMIT,
                                    new BigDecimal("500000"),
                                    new BigDecimal("100000000")),
                            ctx(new BigDecimal("100000000")),
                            NOW);
            order.fill(new BigDecimal("100000000"), LocalDateTime.now());

            assertThat(order.isPending()).isFalse();
        }
    }
}
