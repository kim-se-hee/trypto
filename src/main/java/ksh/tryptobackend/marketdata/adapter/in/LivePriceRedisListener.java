package ksh.tryptobackend.marketdata.adapter.in;

import jakarta.annotation.PostConstruct;
import ksh.tryptobackend.marketdata.adapter.in.dto.LivePriceMessage;
import ksh.tryptobackend.marketdata.adapter.in.dto.response.LivePriceResponse;
import ksh.tryptobackend.marketdata.application.port.in.FindAllExchangeIdsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LivePriceRedisListener {

    private static final String CHANNEL_PREFIX = "prices.";
    private static final String TOPIC_PREFIX = "/topic/prices.";

    private final RedisMessageListenerContainer listenerContainer;
    private final SimpMessagingTemplate messagingTemplate;
    private final FindAllExchangeIdsUseCase findAllExchangeIdsUseCase;
    private final ObjectMapper objectMapper;

    @PostConstruct
    void subscribeAll() {
        List<Long> exchangeIds = findAllExchangeIdsUseCase.findAllExchangeIds();
        for (Long exchangeId : exchangeIds) {
            String channel = CHANNEL_PREFIX + exchangeId;
            String topic = TOPIC_PREFIX + exchangeId;

            listenerContainer.addMessageListener(
                    createListener(topic),
                    new ChannelTopic(channel)
            );

            log.info("Redis Pub/Sub 구독 등록: channel={}, topic={}", channel, topic);
        }
    }

    private MessageListener createListener(String topic) {
        return (Message message, byte[] pattern) -> {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            try {
                LivePriceMessage msg = objectMapper.readValue(body, LivePriceMessage.class);
                LivePriceResponse response = new LivePriceResponse(
                        msg.coinId(), msg.symbol(), msg.price(), msg.changeRate(), msg.timestamp());
                messagingTemplate.convertAndSend(topic, response);
            } catch (JacksonException e) {
                log.warn("시세 메시지 파싱 실패: {}", body, e);
            }
        };
    }

}
