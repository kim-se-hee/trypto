package ksh.tryptobackend.trading.adapter.in;

import io.micrometer.core.instrument.MeterRegistry;
import ksh.tryptobackend.common.config.RabbitMqConfig;
import ksh.tryptobackend.trading.adapter.in.messages.MatchedOrderMessage;
import ksh.tryptobackend.trading.application.port.in.FillPendingOrderUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MatchedOrderEventListener {

    private static final String MATCHED_ORDERS_RETRY_QUEUE = "matched.orders.retry";
    private static final String MATCHED_ORDERS_DLQ = "matched.orders.dlq";

    private final FillPendingOrderUseCase fillPendingOrderUseCase;
    private final RabbitTemplate rabbitTemplate;
    private final RetryTemplate mainRetryTemplate;
    private final RetryTemplate retryTierRetryTemplate;
    private final MeterRegistry meterRegistry;

    public MatchedOrderEventListener(FillPendingOrderUseCase fillPendingOrderUseCase,
                                     RabbitTemplate rabbitTemplate,
                                     @Qualifier(RabbitMqConfig.MATCHED_ORDERS_MAIN_RETRY_TEMPLATE) RetryTemplate mainRetryTemplate,
                                     @Qualifier(RabbitMqConfig.MATCHED_ORDERS_RETRY_TIER_RETRY_TEMPLATE) RetryTemplate retryTierRetryTemplate,
                                     MeterRegistry meterRegistry) {
        this.fillPendingOrderUseCase = fillPendingOrderUseCase;
        this.rabbitTemplate = rabbitTemplate;
        this.mainRetryTemplate = mainRetryTemplate;
        this.retryTierRetryTemplate = retryTierRetryTemplate;
        this.meterRegistry = meterRegistry;
    }

    @RabbitListener(
        queues = "matched.orders",
        id = RabbitMqConfig.MATCHED_ORDERS_LISTENER_ID,
        containerFactory = RabbitMqConfig.MATCHED_ORDERS_CONTAINER_FACTORY
    )
    public void handleMatchedOrders(MatchedOrderMessage message) {
        long receivedAtMs = System.currentTimeMillis();
        meterRegistry.timer("match.queue.wait")
                .record(receivedAtMs - message.publishedAtMs(), TimeUnit.MILLISECONDS);

        for (MatchedOrderMessage.Item item : message.matched()) {
            fillWithMainRetry(item, message.tickerTsMs());
        }
    }

    @RabbitListener(
        queues = MATCHED_ORDERS_RETRY_QUEUE,
        id = RabbitMqConfig.MATCHED_ORDERS_RETRY_LISTENER_ID,
        containerFactory = RabbitMqConfig.MATCHED_ORDERS_RETRY_CONTAINER_FACTORY
    )
    public void handleRetry(MatchedOrderMessage.Item item) {
        fillWithRetryTierRetry(item);
    }

    private void fillWithMainRetry(MatchedOrderMessage.Item item, long tickerTsMs) {
        try {
            mainRetryTemplate.execute(context -> {
                fillPendingOrderUseCase.fillOrder(item.orderId(), item.filledPrice());
                return null;
            });
            meterRegistry.timer("match.fill.e2e")
                    .record(System.currentTimeMillis() - tickerTsMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("메인 재시도 소진, retry 큐 발행: orderId={}", item.orderId(), e);
            publishToRetryQueue(item);
        }
    }

    private void fillWithRetryTierRetry(MatchedOrderMessage.Item item) {
        try {
            retryTierRetryTemplate.execute(context -> {
                fillPendingOrderUseCase.fillOrder(item.orderId(), item.filledPrice());
                return null;
            });
        } catch (Exception e) {
            log.error("retry 재시도 소진, DLQ 발행: orderId={}", item.orderId(), e);
            publishToDlq(item);
        }
    }

    private void publishToRetryQueue(MatchedOrderMessage.Item item) {
        try {
            rabbitTemplate.convertAndSend(MATCHED_ORDERS_RETRY_QUEUE, item);
        } catch (Exception e) {
            log.error("retry 큐 발행 실패: orderId={}", item.orderId(), e);
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
