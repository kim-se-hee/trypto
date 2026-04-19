package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinMappingResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.trading.application.port.out.HoldingCommandPort;
import ksh.tryptobackend.trading.application.port.out.OrderCommandPort;
import ksh.tryptobackend.trading.application.port.out.OrderFilledEventPort;
import ksh.tryptobackend.trading.domain.model.Holding;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.Fee;
import ksh.tryptobackend.trading.domain.vo.OrderFilledEvent;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.wallet.application.port.in.GetWalletOwnerIdUseCase;
import ksh.tryptobackend.wallet.application.port.in.ManageWalletBalanceUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FillPendingOrderServiceTest {

    @Mock private OrderCommandPort orderCommandPort;
    @Mock private HoldingCommandPort holdingCommandPort;
    @Mock private FindExchangeCoinMappingUseCase findExchangeCoinMappingUseCase;
    @Mock private FindExchangeDetailUseCase findExchangeDetailUseCase;
    @Mock private ManageWalletBalanceUseCase manageWalletBalanceUseCase;
    @Mock private GetWalletOwnerIdUseCase getWalletOwnerIdUseCase;
    @Mock private OrderFilledEventPort orderFilledEventPort;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-17T12:00:00Z"), ZoneId.of("UTC"));

    private FillPendingOrderService sut;

    private static final Long ORDER_ID = 1L;
    private static final Long WALLET_ID = 10L;
    private static final Long EXCHANGE_COIN_ID = 100L;
    private static final Long EXCHANGE_ID = 200L;
    private static final Long COIN_ID = 300L;
    private static final Long BASE_CURRENCY_COIN_ID = 1L;
    private static final BigDecimal CURRENT_PRICE = new BigDecimal("50000000");

    @BeforeEach
    void setUp() {
        sut = new FillPendingOrderService(
            orderCommandPort, holdingCommandPort,
            findExchangeCoinMappingUseCase, findExchangeDetailUseCase,
            manageWalletBalanceUseCase, getWalletOwnerIdUseCase,
            orderFilledEventPort, clock
        );
    }

    @Nested
    @DisplayName("지정가 매수 체결")
    class FillBuyOrderTest {

        @Test
        @DisplayName("정상 체결 - 기준통화 unlock/deduct 후 코인 add")
        void fillOrder_buyOrder_settlesBalanceAndUpdatesHolding() {
            // Given
            Order order = createFilledBuyOrder();
            stubDependencies(order);
            when(holdingCommandPort.findByWalletIdAndCoinId(WALLET_ID, COIN_ID))
                .thenReturn(Optional.of(Holding.empty(WALLET_ID, COIN_ID)));
            when(holdingCommandPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            sut.fillOrder(ORDER_ID, CURRENT_PRICE);

            // Then
            BigDecimal settlementDebit = order.getSettlementDebit();
            verify(manageWalletBalanceUseCase).unlockBalance(WALLET_ID, BASE_CURRENCY_COIN_ID, settlementDebit);
            verify(manageWalletBalanceUseCase).deductBalance(WALLET_ID, BASE_CURRENCY_COIN_ID, settlementDebit);
            verify(manageWalletBalanceUseCase).addBalance(WALLET_ID, COIN_ID, order.getQuantity().value());
        }
    }

    @Nested
    @DisplayName("지정가 매도 체결")
    class FillSellOrderTest {

        @Test
        @DisplayName("정상 체결 - 코인 unlock/deduct 후 기준통화 add")
        void fillOrder_sellOrder_settlesBalanceAndUpdatesHolding() {
            // Given
            Order order = createFilledSellOrder();
            stubDependencies(order);
            when(holdingCommandPort.findByWalletIdAndCoinId(WALLET_ID, COIN_ID))
                .thenReturn(Optional.of(createHoldingWithQuantity(new BigDecimal("1"))));
            when(holdingCommandPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            sut.fillOrder(ORDER_ID, CURRENT_PRICE);

            // Then
            BigDecimal unlockQuantity = order.getQuantity().value();
            verify(manageWalletBalanceUseCase).unlockBalance(WALLET_ID, COIN_ID, unlockQuantity);
            verify(manageWalletBalanceUseCase).deductBalance(WALLET_ID, COIN_ID, unlockQuantity);
            verify(manageWalletBalanceUseCase).addBalance(WALLET_ID, BASE_CURRENCY_COIN_ID, order.getSettlementCredit());
        }
    }

    @Nested
    @DisplayName("체결 스킵 케이스")
    class SkipCaseTest {

        @Test
        @DisplayName("CAS 실패 시 (이미 처리된 주문) 체결하지 않고 종료")
        void fillOrder_casFails_skips() {
            // Given
            when(orderCommandPort.fillOrder(anyLong(), any())).thenReturn(false);

            // When
            sut.fillOrder(ORDER_ID, CURRENT_PRICE);

            // Then
            verify(manageWalletBalanceUseCase, never()).unlockBalance(any(), any(), any());
            verify(orderCommandPort, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("체결 이벤트 발행")
    class EventPublishTest {

        @Test
        @DisplayName("체결 성공 후 OrderFilledEvent가 발행된다")
        void fillOrder_success_publishesEvent() {
            // Given
            Order order = createFilledBuyOrder();
            stubDependencies(order);
            when(holdingCommandPort.findByWalletIdAndCoinId(WALLET_ID, COIN_ID))
                .thenReturn(Optional.of(Holding.empty(WALLET_ID, COIN_ID)));
            when(holdingCommandPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(getWalletOwnerIdUseCase.getWalletOwnerId(WALLET_ID)).thenReturn(999L);

            // When
            sut.fillOrder(ORDER_ID, CURRENT_PRICE);

            // Then
            verify(orderFilledEventPort).publish(argThat(event ->
                event instanceof OrderFilledEvent
                    && event.userId().equals(999L)
                    && event.orderId().equals(ORDER_ID)));
        }
    }

    private void stubDependencies(Order order) {
        when(orderCommandPort.fillOrder(anyLong(), any())).thenReturn(true);
        when(orderCommandPort.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(findExchangeCoinMappingUseCase.findById(EXCHANGE_COIN_ID))
            .thenReturn(Optional.of(new ExchangeCoinMappingResult(EXCHANGE_COIN_ID, EXCHANGE_ID, COIN_ID)));
        when(findExchangeDetailUseCase.findExchangeDetail(EXCHANGE_ID))
            .thenReturn(Optional.of(new ExchangeDetailResult("UPBIT", BASE_CURRENCY_COIN_ID, true, new BigDecimal("0.0005"))));
    }

    private Order createFilledBuyOrder() {
        return Order.reconstitute(
            ORDER_ID, "key-1", 1L, WALLET_ID, EXCHANGE_COIN_ID, COIN_ID, BASE_CURRENCY_COIN_ID,
            Side.BUY, OrderType.LIMIT, new BigDecimal("500000"),
            new Quantity(new BigDecimal("0.01")), new BigDecimal("50000000"),
            new BigDecimal("50000000"), Fee.of(new BigDecimal("250"), new BigDecimal("0.0005")),
            OrderStatus.FILLED, LocalDateTime.now(), LocalDateTime.now(), null
        );
    }

    private Order createFilledSellOrder() {
        return Order.reconstitute(
            ORDER_ID, "key-2", 1L, WALLET_ID, EXCHANGE_COIN_ID, COIN_ID, BASE_CURRENCY_COIN_ID,
            Side.SELL, OrderType.LIMIT, new BigDecimal("500000"),
            new Quantity(new BigDecimal("0.01")), new BigDecimal("50000000"),
            new BigDecimal("50000000"), Fee.of(new BigDecimal("250"), new BigDecimal("0.0005")),
            OrderStatus.FILLED, LocalDateTime.now(), LocalDateTime.now(), null
        );
    }

    private Holding createHoldingWithQuantity(BigDecimal quantity) {
        return Holding.builder()
            .walletId(WALLET_ID)
            .coinId(COIN_ID)
            .avgBuyPrice(new BigDecimal("48000000"))
            .totalQuantity(quantity)
            .totalBuyAmount(quantity.multiply(new BigDecimal("48000000")))
            .averagingDownCount(0)
            .build();
    }
}
