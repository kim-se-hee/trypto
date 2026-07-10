package ksh.tryptobackend.trading.adapter.out.messaging;

import ksh.tryptobackend.trading.domain.event.OrderCanceledEvent;
import ksh.tryptobackend.trading.domain.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class EngineInboxPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${engine.inbox.queue:engine.inbox}")
    private String inboxQueue;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        if (!event.awaitsMatching()) return;
        OrderPlacedEngineMessage payload = OrderPlacedEngineMessage.from(event);
        publish("OrderPlaced", payload, payload.orderId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCanceled(OrderCanceledEvent event) {
        OrderCanceledEngineMessage payload = OrderCanceledEngineMessage.from(event);
        publish("OrderCanceled", payload, payload.orderId());
    }

    private void publish(String type, Object payload, Long orderId) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(payload);
            Message msg =
                    MessageBuilder.withBody(body)
                            .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                            .setDeliveryMode(MessageProperties.DEFAULT_DELIVERY_MODE)
                            .setHeader("event_type", type)
                            .build();
            rabbitTemplate.send("", inboxQueue, msg);
        } catch (Exception e) {
            log.warn("engine.inbox publish failed type={} orderId={}", type, orderId, e);
        }
    }
}
