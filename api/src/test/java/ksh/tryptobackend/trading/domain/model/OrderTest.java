package ksh.tryptobackend.trading.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.domain.event.OrderCanceledEvent;
import ksh.tryptobackend.trading.domain.event.OrderFilledEvent;
import ksh.tryptobackend.trading.domain.event.OrderPlacedEvent;
import ksh.tryptobackend.trading.domain.vo.ExchangeInfo;
import ksh.tryptobackend.trading.domain.vo.Fill;
import ksh.tryptobackend.trading.domain.vo.MarketInfo;
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
            new ExchangeInfo(new BigDecimal("0.0005"), new BigDecimal("5000"), new BigDecimal("1000000000"));
    private static final TradingPair PAIR = new TradingPair(100L, 1L, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 17, 12, 0, 0);

    private static PlaceOrderCommand cmd(Side side, OrderType orderType, BigDecimal volume, BigDecimal price) {
        return new PlaceOrderCommand(UUID.randomUUID().toString(), 1L, 1L, 1L, side, orderType, volume, price);
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
    @DisplayName("주문 입력 검증")
    class OrderInputValidationTest {

        @Test
        @DisplayName("시장가 매수 — price(주문 총액) 누락이면 PRICE_REQUIRED")
        void marketBuy_withoutPrice_throws() {
            assertThatThrownBy(() -> Order.create(
                            cmd(Side.BUY, OrderType.MARKET, null, null), ctx(new BigDecimal("100274000")), NOW))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PRICE_REQUIRED);
        }

        @Test
        @DisplayName("시장가 매수 — volume을 보내면 VOLUME_NOT_ALLOWED")
        void marketBuy_withVolume_throws() {
            assertThatThrownBy(() -> Order.create(
                            cmd(Side.BUY, OrderType.MARKET, new BigDecimal("0.001"), new BigDecimal("100000")),
                            ctx(new BigDecimal("100274000")),
                            NOW))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VOLUME_NOT_ALLOWED);
        }

        @Test
        @DisplayName("시장가 매도 — volume 누락이면 VOLUME_REQUIRED")
        void marketSell_withoutVolume_throws() {
            assertThatThrownBy(() -> Order.create(
                            cmd(Side.SELL, OrderType.MARKET, null, null), ctx(new BigDecimal("100274000")), NOW))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VOLUME_REQUIRED);
        }

        @Test
        @DisplayName("시장가 매도 — price를 보내면 PRICE_NOT_ALLOWED")
        void marketSell_withPrice_throws() {
            assertThatThrownBy(() -> Order.create(
                            cmd(Side.SELL, OrderType.MARKET, new BigDecimal("0.001"), new BigDecimal("100000000")),
                            ctx(new BigDecimal("100274000")),
                            NOW))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PRICE_NOT_ALLOWED);
        }

        @Test
        @DisplayName("지정가 — volume 누락이면 VOLUME_REQUIRED")
        void limit_withoutVolume_throws() {
            assertThatThrownBy(() -> Order.create(
                            cmd(Side.BUY, OrderType.LIMIT, null, new BigDecimal("100000000")),
                            ctx(new BigDecimal("100000000")),
                            NOW))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.VOLUME_REQUIRED);
        }

        @Test
        @DisplayName("지정가 — price 누락이면 PRICE_REQUIRED")
        void limit_withoutPrice_throws() {
            assertThatThrownBy(() -> Order.create(
                            cmd(Side.BUY, OrderType.LIMIT, new BigDecimal("0.005"), null),
                            ctx(new BigDecimal("100000000")),
                            NOW))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PRICE_REQUIRED);
        }

        @Test
        @DisplayName("시장가 매도 — 주문 금액(volume × 현재가)이 최소 미달이면 BELOW_MIN_ORDER_AMOUNT")
        void marketSell_belowMinOrderAmount_throws() {
            assertThatThrownBy(() -> Order.create(
                            cmd(Side.SELL, OrderType.MARKET, new BigDecimal("0.00000001"), null),
                            ctx(new BigDecimal("100274000")),
                            NOW))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.BELOW_MIN_ORDER_AMOUNT);
        }

        @Test
        @DisplayName("지정가 매수 — 주문 금액(volume × 지정가)이 최소 미달이면 BELOW_MIN_ORDER_AMOUNT")
        void limitBuy_belowMinOrderAmount_throws() {
            assertThatThrownBy(() -> Order.create(
                            cmd(Side.BUY, OrderType.LIMIT, new BigDecimal("0.00000001"), new BigDecimal("100000000")),
                            ctx(new BigDecimal("100000000")),
                            NOW))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.BELOW_MIN_ORDER_AMOUNT);
        }
    }

    @Nested
    @DisplayName("체결 금액 계산")
    class FilledAmountCalculationTest {

        @Test
        @DisplayName("시장가 매수 — 체결 금액은 체결 수량 × 현재가")
        void createMarketBuyOrder_filledAmount_equalsQuantityTimesPrice() {
            BigDecimal totalPrice = new BigDecimal("100000");
            BigDecimal currentPrice = new BigDecimal("100274000");

            Order order = Order.create(cmd(Side.BUY, OrderType.MARKET, null, totalPrice), ctx(currentPrice), NOW);

            BigDecimal expectedAmount = order.getQuantity().value().multiply(currentPrice);
            assertThat(order.getFilledAmount().value()).isEqualByComparingTo(expectedAmount);
        }

        @Test
        @DisplayName("시장가 매도 — 체결 금액은 매도 수량 × 현재가")
        void createMarketSellOrder_filledAmount_equalsQuantityTimesPrice() {
            BigDecimal sellQuantity = new BigDecimal("0.5");
            BigDecimal currentPrice = new BigDecimal("100274000");

            Order order = Order.create(cmd(Side.SELL, OrderType.MARKET, sellQuantity, null), ctx(currentPrice), NOW);

            BigDecimal expectedAmount = sellQuantity.multiply(currentPrice);
            assertThat(order.getFilledAmount().value()).isEqualByComparingTo(expectedAmount);
        }

        @Test
        @DisplayName("지정가 매수 — 미체결이면 체결 금액·fee는 null, 체결 후 체결가 기준으로 확정")
        void createLimitBuyOrder_filledAmountAndFee_realizedOnFill() {
            BigDecimal volume = new BigDecimal("0.005");
            BigDecimal limitPrice = new BigDecimal("100000000");
            BigDecimal executionPrice = new BigDecimal("99000000");

            Order order = Order.create(cmd(Side.BUY, OrderType.LIMIT, volume, limitPrice), ctx(limitPrice), NOW);

            assertThat(order.getFilledAmount()).isNull();
            assertThat(order.getFeeAmount()).isNull();

            order.fill(Price.of(executionPrice), PAIR.quoteScale(), NOW);

            // 체결 금액 495000 × 0.0005 = 247.5 → KRW(자릿수 0) 내림 절삭 247
            BigDecimal expectedAmount = order.getQuantity().value().multiply(executionPrice);
            assertThat(order.getFilledAmount().value()).isEqualByComparingTo(expectedAmount);
            assertThat(order.getFeeAmount().value()).isEqualByComparingTo(new BigDecimal("247"));
        }

        @Test
        @DisplayName("지정가 매도 — 미체결이면 체결 금액은 null, 체결 후 체결가 × 수량")
        void createLimitSellOrder_filledAmount_realizedOnFill() {
            BigDecimal sellQuantity = new BigDecimal("0.001");
            BigDecimal limitPrice = new BigDecimal("110000000");
            BigDecimal executionPrice = new BigDecimal("111000000");

            Order order = Order.create(cmd(Side.SELL, OrderType.LIMIT, sellQuantity, limitPrice), ctx(limitPrice), NOW);

            assertThat(order.getFilledAmount()).isNull();

            order.fill(Price.of(executionPrice), PAIR.quoteScale(), NOW);

            BigDecimal expectedAmount = sellQuantity.multiply(executionPrice);
            assertThat(order.getFilledAmount().value()).isEqualByComparingTo(expectedAmount);
        }
    }

    @Nested
    @DisplayName("수수료 계산")
    class FeeCalculationTest {

        @Test
        @DisplayName("정상 수수료 계산 — 체결 금액(가격 × 수량) × 수수료율, 기축통화 자릿수 8이면 그대로")
        void settle_normal_correctFee() {
            Fill fill = Fill.settle(
                    Price.of(new BigDecimal("99726.44")),
                    Quantity.of(BigDecimal.ONE),
                    new BigDecimal("0.0005"),
                    8,
                    NOW);

            assertThat(fill.fee().value()).isEqualByComparingTo(new BigDecimal("49.86322000"));
        }

        @Test
        @DisplayName("기축통화 자릿수 0(KRW) — 수수료를 정수로 내림 절삭")
        void settle_zeroScale_feeFlooredToInteger() {
            Fill fill = Fill.settle(
                    Price.of(new BigDecimal("99726.44")),
                    Quantity.of(BigDecimal.ONE),
                    new BigDecimal("0.0005"),
                    0,
                    NOW);

            assertThat(fill.fee().value()).isEqualByComparingTo(new BigDecimal("49"));
        }

        @Test
        @DisplayName("수수료율 0% — 수수료 0")
        void settle_zeroRate_zeroFee() {
            Fill fill = Fill.settle(
                    Price.of(new BigDecimal("1000000")), Quantity.of(BigDecimal.ONE), BigDecimal.ZERO, 8, NOW);

            assertThat(fill.fee().value()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("주문 체결")
    class FillTest {

        @Test
        @DisplayName("PENDING 주문 체결 성공 - 상태가 FILLED로 변경되고 filledAt이 설정된다")
        void fill_pendingOrder_filledSuccessfully() {
            Order order = Order.create(
                    cmd(Side.BUY, OrderType.LIMIT, new BigDecimal("0.005"), new BigDecimal("100000000")),
                    ctx(new BigDecimal("100000000")),
                    NOW);
            LocalDateTime fillTime = LocalDateTime.of(2026, 3, 17, 12, 0, 0);

            order.fill(Price.of(new BigDecimal("100000000")), PAIR.quoteScale(), fillTime);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
            assertThat(order.getFilledAt()).isEqualTo(fillTime);
        }

        @Test
        @DisplayName("FILLED 주문에 fill 시도 - 예외 발생")
        void fill_filledOrder_throwsException() {
            Order order = Order.create(
                    cmd(Side.BUY, OrderType.MARKET, null, new BigDecimal("100000")),
                    ctx(new BigDecimal("100274000")),
                    NOW);

            assertThatThrownBy(() ->
                            order.fill(Price.of(new BigDecimal("100274000")), PAIR.quoteScale(), LocalDateTime.now()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("지정가 매수 - 지정가보다 높은 체결가로 fill 시도 - 예외 발생")
        void fill_buyAboveLimitPrice_throwsException() {
            Order order = Order.create(
                    cmd(Side.BUY, OrderType.LIMIT, new BigDecimal("0.005"), new BigDecimal("100000000")),
                    ctx(new BigDecimal("100000000")),
                    NOW);

            assertThatThrownBy(() -> order.fill(Price.of(new BigDecimal("100000001")), PAIR.quoteScale(), NOW))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("지정가 매도 - 지정가보다 낮은 체결가로 fill 시도 - 예외 발생")
        void fill_sellBelowLimitPrice_throwsException() {
            Order order = Order.create(
                    cmd(Side.SELL, OrderType.LIMIT, new BigDecimal("0.001"), new BigDecimal("110000000")),
                    ctx(new BigDecimal("110000000")),
                    NOW);

            assertThatThrownBy(() -> order.fill(Price.of(new BigDecimal("109999999")), PAIR.quoteScale(), NOW))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("주문 취소")
    class CancelTest {

        @Test
        @DisplayName("PENDING 지정가 주문 취소 - 상태가 CANCELED로 바뀌고 취소 이벤트가 등록된다")
        void cancel_pendingOrder_cancelled() {
            Order order = Order.create(
                    cmd(Side.BUY, OrderType.LIMIT, new BigDecimal("0.005"), new BigDecimal("100000000")),
                    ctx(new BigDecimal("100000000")),
                    NOW);
            order.pullDomainEvents();

            order.cancel(1L, PAIR);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
            assertThat(order.pullDomainEvents()).hasSize(1).first().isInstanceOf(OrderCanceledEvent.class);
        }

        @Test
        @DisplayName("체결된 주문 취소 시도 - 예외 발생")
        void cancel_filledOrder_throwsException() {
            Order order = Order.create(
                    cmd(Side.BUY, OrderType.MARKET, null, new BigDecimal("100000")),
                    ctx(new BigDecimal("100274000")),
                    NOW);

            assertThatThrownBy(() -> order.cancel(1L, PAIR)).isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("도메인 이벤트 등록")
    class DomainEventTest {

        @Test
        @DisplayName("시장가 주문 생성 - OrderPlacedEvent와 OrderFilledEvent가 등록된다")
        void create_marketOrder_registersPlacedAndFilledEvents() {
            Order order = Order.create(
                    cmd(Side.BUY, OrderType.MARKET, null, new BigDecimal("100000")),
                    ctx(new BigDecimal("100274000")),
                    NOW);

            assertThat(order.pullDomainEvents())
                    .hasSize(2)
                    .hasExactlyElementsOfTypes(OrderPlacedEvent.class, OrderFilledEvent.class);
        }

        @Test
        @DisplayName("지정가 주문 생성 - OrderPlacedEvent만 등록된다")
        void create_limitOrder_registersPlacedEventOnly() {
            Order order = Order.create(
                    cmd(Side.BUY, OrderType.LIMIT, new BigDecimal("0.005"), new BigDecimal("100000000")),
                    ctx(new BigDecimal("100000000")),
                    NOW);

            assertThat(order.pullDomainEvents()).hasSize(1).hasExactlyElementsOfTypes(OrderPlacedEvent.class);
        }

        @Test
        @DisplayName("이벤트는 한 번 꺼내면 비워진다")
        void pullDomainEvents_clearsEvents() {
            Order order = Order.create(
                    cmd(Side.BUY, OrderType.MARKET, null, new BigDecimal("100000")),
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
            Order order = Order.create(
                    cmd(Side.BUY, OrderType.LIMIT, new BigDecimal("0.005"), new BigDecimal("100000000")),
                    ctx(new BigDecimal("100000000")),
                    NOW);

            assertThat(order.isPending()).isTrue();
        }

        @Test
        @DisplayName("시장가 주문 생성 직후 - isPending이 false")
        void isPending_marketOrder_false() {
            Order order = Order.create(
                    cmd(Side.BUY, OrderType.MARKET, null, new BigDecimal("100000")),
                    ctx(new BigDecimal("100274000")),
                    NOW);

            assertThat(order.isPending()).isFalse();
        }

        @Test
        @DisplayName("체결된 주문 - isPending이 false")
        void isPending_filledOrder_false() {
            Order order = Order.create(
                    cmd(Side.BUY, OrderType.LIMIT, new BigDecimal("0.005"), new BigDecimal("100000000")),
                    ctx(new BigDecimal("100000000")),
                    NOW);
            order.fill(Price.of(new BigDecimal("100000000")), PAIR.quoteScale(), LocalDateTime.now());

            assertThat(order.isPending()).isFalse();
        }
    }
}
