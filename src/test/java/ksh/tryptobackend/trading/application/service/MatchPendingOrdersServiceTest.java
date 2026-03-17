package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.FillPendingOrderUseCase;
import ksh.tryptobackend.trading.application.port.out.ExchangeCoinMappingCacheQueryPort;
import ksh.tryptobackend.trading.application.port.out.OrderFillFailureCommandPort;
import ksh.tryptobackend.trading.application.port.out.PendingOrderCacheCommandPort;
import ksh.tryptobackend.trading.application.port.out.PendingOrderCacheQueryPort;
import ksh.tryptobackend.trading.domain.vo.PendingOrder;
import ksh.tryptobackend.trading.domain.vo.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchPendingOrdersServiceTest {

    @Mock private PendingOrderCacheCommandPort pendingOrderCacheCommandPort;
    @Mock private PendingOrderCacheQueryPort pendingOrderCacheQueryPort;
    @Mock private ExchangeCoinMappingCacheQueryPort exchangeCoinMappingCacheQueryPort;
    @Mock private OrderFillFailureCommandPort orderFillFailureCommandPort;
    @Mock private FillPendingOrderUseCase fillPendingOrderUseCase;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-17T12:00:00Z"), ZoneId.of("UTC"));

    private MatchPendingOrdersService sut;

    private static final String EXCHANGE = "UPBIT";
    private static final String SYMBOL = "BTC/KRW";
    private static final Long EXCHANGE_COIN_ID = 100L;
    private static final BigDecimal CURRENT_PRICE = new BigDecimal("50000000");

    @BeforeEach
    void setUp() {
        sut = new MatchPendingOrdersService(
            pendingOrderCacheCommandPort, pendingOrderCacheQueryPort,
            exchangeCoinMappingCacheQueryPort, orderFillFailureCommandPort,
            fillPendingOrderUseCase, clock
        );
    }

    @Nested
    @DisplayName("매핑 변환")
    class MappingResolutionTest {

        @Test
        @DisplayName("매핑이 없으면 매칭 없이 종료")
        void matchOrders_noMapping_skips() {
            // Given
            when(exchangeCoinMappingCacheQueryPort.resolve(EXCHANGE, SYMBOL))
                .thenReturn(Optional.empty());

            // When
            sut.matchOrders(EXCHANGE, SYMBOL, CURRENT_PRICE);

            // Then
            verify(pendingOrderCacheQueryPort, never()).findMatchedOrders(any(), any());
        }
    }

    @Nested
    @DisplayName("매칭 대상 조회")
    class MatchedOrdersTest {

        @Test
        @DisplayName("매칭 대상이 없으면 체결 없이 종료")
        void matchOrders_noMatchedOrders_skips() {
            // Given
            when(exchangeCoinMappingCacheQueryPort.resolve(EXCHANGE, SYMBOL))
                .thenReturn(Optional.of(EXCHANGE_COIN_ID));
            when(pendingOrderCacheQueryPort.findMatchedOrders(EXCHANGE_COIN_ID, CURRENT_PRICE))
                .thenReturn(Collections.emptyList());

            // When
            sut.matchOrders(EXCHANGE, SYMBOL, CURRENT_PRICE);

            // Then
            verify(fillPendingOrderUseCase, never()).fillOrder(any(), any());
        }

        @Test
        @DisplayName("매칭된 주문이 있으면 캐시에서 제거 후 체결 처리")
        void matchOrders_matchedOrders_removesFromCacheAndFills() {
            // Given
            PendingOrder order1 = new PendingOrder(1L, EXCHANGE_COIN_ID, Side.BUY, new BigDecimal("51000000"));
            PendingOrder order2 = new PendingOrder(2L, EXCHANGE_COIN_ID, Side.BUY, new BigDecimal("52000000"));
            when(exchangeCoinMappingCacheQueryPort.resolve(EXCHANGE, SYMBOL))
                .thenReturn(Optional.of(EXCHANGE_COIN_ID));
            when(pendingOrderCacheQueryPort.findMatchedOrders(EXCHANGE_COIN_ID, CURRENT_PRICE))
                .thenReturn(List.of(order1, order2));

            // When
            sut.matchOrders(EXCHANGE, SYMBOL, CURRENT_PRICE);

            // Then
            verify(pendingOrderCacheCommandPort).remove(EXCHANGE_COIN_ID, 1L);
            verify(pendingOrderCacheCommandPort).remove(EXCHANGE_COIN_ID, 2L);
            verify(fillPendingOrderUseCase).fillOrder(1L, CURRENT_PRICE);
            verify(fillPendingOrderUseCase).fillOrder(2L, CURRENT_PRICE);
        }
    }

    @Nested
    @DisplayName("재시도 및 실패 처리")
    class RetryAndFailureTest {

        @Test
        @DisplayName("낙관적 락 충돌 시 캐시 재추가 없이 skip")
        void matchOrders_optimisticLockFailure_skipsWithoutReAdd() {
            // Given
            PendingOrder order = new PendingOrder(1L, EXCHANGE_COIN_ID, Side.BUY, new BigDecimal("51000000"));
            when(exchangeCoinMappingCacheQueryPort.resolve(EXCHANGE, SYMBOL))
                .thenReturn(Optional.of(EXCHANGE_COIN_ID));
            when(pendingOrderCacheQueryPort.findMatchedOrders(EXCHANGE_COIN_ID, CURRENT_PRICE))
                .thenReturn(List.of(order));
            doThrow(new OptimisticLockingFailureException("version conflict"))
                .when(fillPendingOrderUseCase).fillOrder(1L, CURRENT_PRICE);

            // When
            sut.matchOrders(EXCHANGE, SYMBOL, CURRENT_PRICE);

            // Then
            verify(pendingOrderCacheCommandPort).remove(EXCHANGE_COIN_ID, 1L);
            verify(pendingOrderCacheCommandPort, never()).add(any());
        }

        @Test
        @DisplayName("체결 실패 후 재시도 소진 시 캐시에 재추가하고 실패 이력 기록")
        void matchOrders_retryExhausted_reAddsAndRecordsFailure() {
            // Given
            PendingOrder order = new PendingOrder(1L, EXCHANGE_COIN_ID, Side.BUY, new BigDecimal("51000000"));
            when(exchangeCoinMappingCacheQueryPort.resolve(EXCHANGE, SYMBOL))
                .thenReturn(Optional.of(EXCHANGE_COIN_ID));
            when(pendingOrderCacheQueryPort.findMatchedOrders(EXCHANGE_COIN_ID, CURRENT_PRICE))
                .thenReturn(List.of(order));
            doThrow(new RuntimeException("DB connection timeout"))
                .when(fillPendingOrderUseCase).fillOrder(1L, CURRENT_PRICE);

            // When
            sut.matchOrders(EXCHANGE, SYMBOL, CURRENT_PRICE);

            // Then
            verify(fillPendingOrderUseCase, times(3)).fillOrder(1L, CURRENT_PRICE);
            verify(pendingOrderCacheCommandPort).add(order);
            verify(orderFillFailureCommandPort).save(any());
        }
    }
}
