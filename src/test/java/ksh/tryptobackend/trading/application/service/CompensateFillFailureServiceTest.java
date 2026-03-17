package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.FillPendingOrderUseCase;
import ksh.tryptobackend.trading.application.port.out.OrderCommandPort;
import ksh.tryptobackend.trading.application.port.out.OrderFillFailureCommandPort;
import ksh.tryptobackend.trading.application.port.out.OrderFillFailureQueryPort;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.model.OrderFillFailure;
import ksh.tryptobackend.trading.domain.vo.Fee;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import ksh.tryptobackend.trading.domain.vo.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompensateFillFailureServiceTest {

    @Mock private OrderFillFailureQueryPort orderFillFailureQueryPort;
    @Mock private OrderFillFailureCommandPort orderFillFailureCommandPort;
    @Mock private OrderCommandPort orderCommandPort;
    @Mock private FillPendingOrderUseCase fillPendingOrderUseCase;

    private CompensateFillFailureService sut;

    private static final Long ORDER_ID = 1L;
    private static final BigDecimal ATTEMPTED_PRICE = new BigDecimal("50000000");

    @BeforeEach
    void setUp() {
        sut = new CompensateFillFailureService(
            orderFillFailureQueryPort, orderFillFailureCommandPort,
            orderCommandPort, fillPendingOrderUseCase
        );
    }

    @Nested
    @DisplayName("보상 스케줄러 실행")
    class CompensateTest {

        @Test
        @DisplayName("미해결 실패 이력이 없으면 아무 처리도 하지 않음")
        void compensate_noUnresolved_skips() {
            // Given
            when(orderFillFailureQueryPort.findUnresolved()).thenReturn(Collections.emptyList());

            // When
            sut.compensate();

            // Then
            verify(orderCommandPort, never()).findById(any());
        }

        @Test
        @DisplayName("주문이 삭제된 경우 실패 이력을 resolved로 갱신")
        void compensate_orderNotFound_resolvesFailure() {
            // Given
            OrderFillFailure failure = createUnresolvedFailure();
            when(orderFillFailureQueryPort.findUnresolved()).thenReturn(List.of(failure));
            when(orderCommandPort.findById(ORDER_ID)).thenReturn(Optional.empty());

            // When
            sut.compensate();

            // Then
            assertThat(failure.isResolved()).isTrue();
            verify(orderFillFailureCommandPort).save(failure);
            verify(fillPendingOrderUseCase, never()).fillOrder(any(), any());
        }

        @Test
        @DisplayName("주문이 이미 FILLED 상태이면 실패 이력을 resolved로 갱신")
        void compensate_orderAlreadyFilled_resolvesFailure() {
            // Given
            OrderFillFailure failure = createUnresolvedFailure();
            Order filledOrder = createOrder(OrderStatus.FILLED);
            when(orderFillFailureQueryPort.findUnresolved()).thenReturn(List.of(failure));
            when(orderCommandPort.findById(ORDER_ID)).thenReturn(Optional.of(filledOrder));

            // When
            sut.compensate();

            // Then
            assertThat(failure.isResolved()).isTrue();
            verify(orderFillFailureCommandPort).save(failure);
            verify(fillPendingOrderUseCase, never()).fillOrder(any(), any());
        }

        @Test
        @DisplayName("주문이 CANCELLED 상태이면 실패 이력을 resolved로 갱신")
        void compensate_orderCancelled_resolvesFailure() {
            // Given
            OrderFillFailure failure = createUnresolvedFailure();
            Order cancelledOrder = createOrder(OrderStatus.CANCELLED);
            when(orderFillFailureQueryPort.findUnresolved()).thenReturn(List.of(failure));
            when(orderCommandPort.findById(ORDER_ID)).thenReturn(Optional.of(cancelledOrder));

            // When
            sut.compensate();

            // Then
            assertThat(failure.isResolved()).isTrue();
            verify(fillPendingOrderUseCase, never()).fillOrder(any(), any());
        }

        @Test
        @DisplayName("주문이 PENDING 상태이면 체결 처리 후 resolved로 갱신")
        void compensate_orderPending_fillsAndResolves() {
            // Given
            OrderFillFailure failure = createUnresolvedFailure();
            Order pendingOrder = createOrder(OrderStatus.PENDING);
            when(orderFillFailureQueryPort.findUnresolved()).thenReturn(List.of(failure));
            when(orderCommandPort.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder));

            // When
            sut.compensate();

            // Then
            verify(fillPendingOrderUseCase).fillOrder(ORDER_ID, ATTEMPTED_PRICE);
            assertThat(failure.isResolved()).isTrue();
            verify(orderFillFailureCommandPort).save(failure);
        }

        @Test
        @DisplayName("체결 재시도 실패 시 resolved로 갱신하지 않음")
        void compensate_fillFails_doesNotResolve() {
            // Given
            OrderFillFailure failure = createUnresolvedFailure();
            Order pendingOrder = createOrder(OrderStatus.PENDING);
            when(orderFillFailureQueryPort.findUnresolved()).thenReturn(List.of(failure));
            when(orderCommandPort.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder));
            org.mockito.Mockito.doThrow(new RuntimeException("DB error"))
                .when(fillPendingOrderUseCase).fillOrder(ORDER_ID, ATTEMPTED_PRICE);

            // When
            sut.compensate();

            // Then
            assertThat(failure.isResolved()).isFalse();
            verify(orderFillFailureCommandPort, never()).save(any());
        }
    }

    private OrderFillFailure createUnresolvedFailure() {
        return OrderFillFailure.create(ORDER_ID, ATTEMPTED_PRICE, LocalDateTime.now(), "DB connection timeout");
    }

    private Order createOrder(OrderStatus status) {
        return Order.reconstitute(
            ORDER_ID, "key-1", 10L, 100L,
            Side.BUY, OrderType.LIMIT, new BigDecimal("500000"),
            new Quantity(new BigDecimal("0.01")), new BigDecimal("50000000"),
            new BigDecimal("50000000"), Fee.of(new BigDecimal("250"), new BigDecimal("0.0005")),
            status, 0L, LocalDateTime.now(),
            status == OrderStatus.FILLED ? LocalDateTime.now() : null, null
        );
    }
}
