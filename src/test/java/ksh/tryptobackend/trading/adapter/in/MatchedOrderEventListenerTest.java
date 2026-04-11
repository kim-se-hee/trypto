package ksh.tryptobackend.trading.adapter.in;

import ksh.tryptobackend.trading.adapter.in.messages.MatchedOrderMessage;
import ksh.tryptobackend.trading.application.port.in.FillPendingOrderUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.support.RetryTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MatchedOrderEventListenerTest {

    @Mock private FillPendingOrderUseCase fillPendingOrderUseCase;
    @Mock private RabbitTemplate rabbitTemplate;

    private MatchedOrderEventListener sut;

    private static final Long ORDER_ID_1 = 1L;
    private static final Long ORDER_ID_2 = 2L;
    private static final BigDecimal FILLED_PRICE = new BigDecimal("50000000");
    private static final int MAIN_MAX_ATTEMPTS = 3;
    private static final int RETRY_TIER_MAX_ATTEMPTS = 4;

    @BeforeEach
    void setUp() {
        sut = new MatchedOrderEventListener(
            fillPendingOrderUseCase,
            rabbitTemplate,
            noBackoffRetryTemplate(MAIN_MAX_ATTEMPTS),
            noBackoffRetryTemplate(RETRY_TIER_MAX_ATTEMPTS)
        );
    }

    private static RetryTemplate noBackoffRetryTemplate(int maxAttempts) {
        return RetryTemplate.builder()
            .maxAttempts(maxAttempts)
            .noBackoff()
            .build();
    }

    @Nested
    @DisplayName("메인 큐 정상 체결 처리")
    class MainQueueFillTest {

        @Test
        @DisplayName("매칭된 주문 목록의 각 항목에 대해 fillOrder를 호출한다")
        void fillsEachMatchedItem() {
            MatchedOrderMessage message = new MatchedOrderMessage(List.of(
                new MatchedOrderMessage.Item(ORDER_ID_1, FILLED_PRICE),
                new MatchedOrderMessage.Item(ORDER_ID_2, FILLED_PRICE)
            ));

            sut.handleMatchedOrders(message);

            verify(fillPendingOrderUseCase).fillOrder(ORDER_ID_1, FILLED_PRICE);
            verify(fillPendingOrderUseCase).fillOrder(ORDER_ID_2, FILLED_PRICE);
        }

        @Test
        @DisplayName("빈 매칭 목록이면 fillOrder를 호출하지 않는다")
        void skipsEmptyList() {
            MatchedOrderMessage message = new MatchedOrderMessage(List.of());

            sut.handleMatchedOrders(message);

            verify(fillPendingOrderUseCase, never()).fillOrder(any(), any());
        }
    }

    @Nested
    @DisplayName("메인 재시도 소진 시 retry 큐 발행")
    class MainRetryExhaustionTest {

        @Test
        @DisplayName("fillOrder 재시도가 소진되면 해당 항목을 retry 큐로 발행한다")
        void publishesToRetryQueueOnMainRetryExhaustion() {
            MatchedOrderMessage.Item failItem = new MatchedOrderMessage.Item(ORDER_ID_1, FILLED_PRICE);
            MatchedOrderMessage message = new MatchedOrderMessage(List.of(failItem));

            doThrow(new RuntimeException("DB 오류"))
                .when(fillPendingOrderUseCase).fillOrder(ORDER_ID_1, FILLED_PRICE);

            sut.handleMatchedOrders(message);

            verify(fillPendingOrderUseCase, times(MAIN_MAX_ATTEMPTS)).fillOrder(ORDER_ID_1, FILLED_PRICE);
            verify(rabbitTemplate).convertAndSend(eq("matched.orders.retry"), eq(failItem));
        }

        @Test
        @DisplayName("한 항목이 실패해도 나머지 항목은 정상 처리된다")
        void continuesProcessingAfterFailure() {
            MatchedOrderMessage message = new MatchedOrderMessage(List.of(
                new MatchedOrderMessage.Item(ORDER_ID_1, FILLED_PRICE),
                new MatchedOrderMessage.Item(ORDER_ID_2, FILLED_PRICE)
            ));

            doThrow(new RuntimeException("DB 오류"))
                .when(fillPendingOrderUseCase).fillOrder(ORDER_ID_1, FILLED_PRICE);

            sut.handleMatchedOrders(message);

            verify(fillPendingOrderUseCase, times(MAIN_MAX_ATTEMPTS)).fillOrder(ORDER_ID_1, FILLED_PRICE);
            verify(fillPendingOrderUseCase).fillOrder(ORDER_ID_2, FILLED_PRICE);
        }
    }

    @Nested
    @DisplayName("retry 큐 재시도 처리")
    class RetryQueueTest {

        @Test
        @DisplayName("retry 큐 아이템이 성공하면 DLQ로 가지 않는다")
        void successfulRetryDoesNotPublishToDlq() {
            MatchedOrderMessage.Item item = new MatchedOrderMessage.Item(ORDER_ID_1, FILLED_PRICE);

            sut.handleRetry(item);

            verify(fillPendingOrderUseCase).fillOrder(ORDER_ID_1, FILLED_PRICE);
            verify(rabbitTemplate, never()).convertAndSend(eq("matched.orders.dlq"), any(MatchedOrderMessage.Item.class));
        }

        @Test
        @DisplayName("retry 큐 재시도가 소진되면 해당 항목을 DLQ로 발행한다")
        void publishesToDlqOnRetryTierExhaustion() {
            MatchedOrderMessage.Item failItem = new MatchedOrderMessage.Item(ORDER_ID_1, FILLED_PRICE);

            doThrow(new RuntimeException("DB 오류"))
                .when(fillPendingOrderUseCase).fillOrder(ORDER_ID_1, FILLED_PRICE);

            sut.handleRetry(failItem);

            verify(fillPendingOrderUseCase, times(RETRY_TIER_MAX_ATTEMPTS)).fillOrder(ORDER_ID_1, FILLED_PRICE);
            verify(rabbitTemplate).convertAndSend(eq("matched.orders.dlq"), eq(failItem));
        }
    }
}
