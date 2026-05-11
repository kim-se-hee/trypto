package ksh.tryptocollector.distribute.rabbitmq;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import ksh.tryptocollector.model.NormalizedTicker;
import ksh.tryptocollector.model.TickerBatchEvent;
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
public class TickerEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public void publishBatch(String exchange, List<NormalizedTicker> tickers) {
        if (tickers.isEmpty()) {
            return;
        }
        try {
            TickerBatchEvent batch = new TickerBatchEvent(
                    exchange,
                    tickers.stream().map(TickerBatchEvent.Item::from).toList()
            );
            byte[] body;
            try {
                body = objectMapper.writeValueAsBytes(batch);
            } catch (JacksonException e) {
                log.error("시세 배치 직렬화 실패: exchange={}, size={}", exchange, tickers.size(), e);
                return;
            }
            Message message = MessageBuilder.withBody(body)
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .build();
            rabbitTemplate.send(RabbitMQConfig.TICKER_EXCHANGE, "", message);
            Counter.builder("rabbitmq.publish")
                    .tag("exchange", exchange)
                    .register(meterRegistry)
                    .increment();
            Counter.builder("rabbitmq.publish.ticker.size")
                    .tag("exchange", exchange)
                    .register(meterRegistry)
                    .increment(tickers.size());
        } catch (Exception e) {
            log.error("시세 배치 발행 실패: exchange={}, size={}", exchange, tickers.size(), e);
        }
    }
}
