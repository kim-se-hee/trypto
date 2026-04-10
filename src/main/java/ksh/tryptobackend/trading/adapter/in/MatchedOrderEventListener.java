package ksh.tryptobackend.trading.adapter.in;

import ksh.tryptobackend.common.config.RabbitMqConfig;
import ksh.tryptobackend.trading.adapter.in.messages.MatchedOrderMessage;
import ksh.tryptobackend.trading.application.port.in.FillPendingOrderUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MatchedOrderEventListener {

    private static final String MATCHED_ORDERS_DLQ = "matched.orders.dlq";

    private final FillPendingOrderUseCase fillPendingOrderUseCase;
    private final RabbitTemplate rabbitTemplate;
    private final RetryTemplate retryTemplate;

    public MatchedOrderEventListener(FillPendingOrderUseCase fillPendingOrderUseCase,
                                     RabbitTemplate rabbitTemplate) {
        this.fillPendingOrderUseCase = fillPendingOrderUseCase;
        this.rabbitTemplate = rabbitTemplate;
        this.retryTemplate = RetryTemplate.builder()
            .maxAttempts(3)
            .exponentialBackoff(1_000L, 3.0, 10_000L)
            .build();
    }

    @RabbitListener(
        queues = "matched.orders",
        id = RabbitMqConfig.MATCHED_ORDERS_LISTENER_ID,
        containerFactory = RabbitMqConfig.MATCHED_ORDERS_CONTAINER_FACTORY
    )
    public void handleMatchedOrders(MatchedOrderMessage message) {
        for (MatchedOrderMessage.Item item : message.matched()) {
            fillWithRetry(item);
        }
    }

    private void fillWithRetry(MatchedOrderMessage.Item item) {
        try {
            retryTemplate.execute(context ->  {
                fillPendingOrderUseCase.fillOrder(item.orderId(), item.filledPrice());
                return null;
            });
        } catch (Exception e) {
            log.error("체결 재시도 소진, DLQ 발행: orderId={}", item.orderId(), e);
            publishToDlq(item);
        }
    }

    private void publishToDlq(MatchedOrderMessage.Item item) {
        try {
            rabbitTemplate.convertAndSend(MATCHED_ORDERS_DLQ, item);
        } catch (Exception e) {
            log.error("DLQ 발행 실패: orderId={}", item.orderId(), e);
        }
    }
}
