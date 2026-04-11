package ksh.tryptobackend.trading.adapter.in;

import ksh.tryptobackend.acceptance.MockAdapterConfiguration;
import ksh.tryptobackend.acceptance.TestContainerConfiguration;
import ksh.tryptobackend.trading.adapter.in.messages.MatchedOrderMessage;
import ksh.tryptobackend.trading.application.port.in.FillPendingOrderUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainerConfiguration.class, MockAdapterConfiguration.class})
@DisplayName("MatchedOrderEventListener RabbitMQ 통합 테스트")
class MatchedOrderEventListenerIntegrationTest {

    private static final String QUEUE_NAME = "matched.orders";
    private static final String RETRY_QUEUE_NAME = "matched.orders.retry";
    private static final String DLQ_NAME = "matched.orders.dlq";
    private static final String DLX_NAME = "matched.orders.dlx";
    private static final String DLQ_ROUTING_KEY = "matched.orders.dlq";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitListenerEndpointRegistry listenerRegistry;

    @MockitoBean
    private FillPendingOrderUseCase fillPendingOrderUseCase;

    @BeforeEach
    void declareQueueAndDrain() {
        // 컬렉터가 선언하는 것과 동일한 spec으로 매칭 주문 큐 선언
        // (백엔드는 missingQueuesFatal=false 이므로 큐가 없어도 컨텍스트는 로드된다)
        Queue queue = QueueBuilder.durable(QUEUE_NAME)
                .quorum()
                .withArgument("x-delivery-limit", 2)
                .deadLetterExchange(DLX_NAME)
                .deadLetterRoutingKey(DLQ_ROUTING_KEY)
                .build();
        rabbitAdmin.declareQueue(queue);

        // 이전 테스트 흔적 비우기
        rabbitAdmin.purgeQueue(QUEUE_NAME);
        rabbitAdmin.purgeQueue(RETRY_QUEUE_NAME);
        rabbitAdmin.purgeQueue(DLQ_NAME);
    }

    @Test
    @DisplayName("컨텍스트 로드 시 missingQueuesFatal=false 덕분에 큐가 없어도 listener 컨테이너가 기동된다")
    void listenerContainerStartsEvenWhenQueueInitiallyMissing() {
        // @SpringBootTest 자체가 컨텍스트 로드를 수행하며, 컨텍스트 로드 시점에는
        // matched.orders 큐가 존재하지 않았음에도 여기 도달했다는 것은
        // missingQueuesFatal=false 설정이 정상 동작함을 의미한다.
        var container = listenerRegistry.getListenerContainer("matchedOrdersListener");
        assertThat(container).isNotNull();

        var retryContainer = listenerRegistry.getListenerContainer("matchedOrdersRetryListener");
        assertThat(retryContainer).isNotNull();
    }

    @Test
    @DisplayName("큐로 publish한 MatchedOrderMessage는 listener가 fillOrder로 전달한다")
    void publishedMessageIsConsumedAndRoutedToFillOrder() {
        willDoNothing().given(fillPendingOrderUseCase).fillOrder(any(), any());

        MatchedOrderMessage message = new MatchedOrderMessage(List.of(
                new MatchedOrderMessage.Item(1001L, new BigDecimal("50000000")),
                new MatchedOrderMessage.Item(1002L, new BigDecimal("50100000"))
        ));

        // RabbitTemplate은 자동 구성된 JsonMessageConverter를 사용하여 큐로 직접 전송
        rabbitTemplate.convertAndSend("", QUEUE_NAME, message);

        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    verify(fillPendingOrderUseCase).fillOrder(eq(1001L), eq(new BigDecimal("50000000")));
                    verify(fillPendingOrderUseCase).fillOrder(eq(1002L), eq(new BigDecimal("50100000")));
                });
    }

    @Test
    @DisplayName("메인 재시도 소진 후 retry 큐를 거쳐 최종적으로 DLQ에 도달한다")
    void failingItemTraversesRetryQueueAndReachesDlq() {
        willThrow(new RuntimeException("DB 오류"))
                .given(fillPendingOrderUseCase).fillOrder(eq(2001L), any());

        MatchedOrderMessage message = new MatchedOrderMessage(List.of(
                new MatchedOrderMessage.Item(2001L, new BigDecimal("50000000"))
        ));
        rabbitTemplate.convertAndSend("", QUEUE_NAME, message);

        // main 3회 (100ms, 500ms) + retry tier 4회 (2s, 4s, 8s)
        // 총 fillOrder 호출 = 7회, 총 소요 ~15초
        // 여유 있게 최대 35초 대기
        await().atMost(Duration.ofSeconds(35))
                .untilAsserted(() -> verify(fillPendingOrderUseCase, times(7))
                        .fillOrder(eq(2001L), any()));

        // DLQ에 메시지가 최종 도달했는지 확인
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    long depth = rabbitAdmin.getQueueInfo(DLQ_NAME).getMessageCount();
                    assertThat(depth).isGreaterThanOrEqualTo(1);
                });
    }

    @Test
    @DisplayName("한 메시지 내 일부 항목이 실패해도 나머지 항목은 fillOrder가 정상 호출된다")
    void successfulItemsAreProcessedEvenWhenOneFails() {
        willThrow(new RuntimeException("DB 오류"))
                .given(fillPendingOrderUseCase).fillOrder(eq(3001L), any());
        willDoNothing().given(fillPendingOrderUseCase).fillOrder(eq(3002L), any());

        MatchedOrderMessage message = new MatchedOrderMessage(List.of(
                new MatchedOrderMessage.Item(3001L, new BigDecimal("50000000")),
                new MatchedOrderMessage.Item(3002L, new BigDecimal("50100000"))
        ));
        rabbitTemplate.convertAndSend("", QUEUE_NAME, message);

        // 3001은 main 3회 실패 후 retry 큐로, 3002는 즉시 성공
        // 메인 시도 3회 + retry tier 최소 1회 = 4회 이상
        await().atMost(Duration.ofSeconds(35))
                .untilAsserted(() -> {
                    verify(fillPendingOrderUseCase, atLeast(3)).fillOrder(eq(3001L), any());
                    verify(fillPendingOrderUseCase).fillOrder(eq(3002L), any());
                });
    }
}
