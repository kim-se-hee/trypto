package ksh.tryptocollector.distribute.rabbitmq;

import ksh.tryptocollector.model.MarketStatusChanged;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketStatusPublisher {

    private static final String FANOUT_ROUTING_KEY = "";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void publish(MarketStatusChanged event) {
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(event);
        } catch (JacksonException e) {
            log.error("상장 상태 변화 직렬화 실패: {}", event, e);
            return;
        }
        Message message = MessageBuilder.withBody(body)
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();
        rabbitTemplate.send(RabbitMQConfig.MARKET_STATUS_EXCHANGE, FANOUT_ROUTING_KEY, message);
        log.info("상장 상태 변화 발행: {}", event);
    }
}
