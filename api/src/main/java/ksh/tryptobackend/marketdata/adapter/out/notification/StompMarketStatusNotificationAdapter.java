package ksh.tryptobackend.marketdata.adapter.out.notification;

import ksh.tryptobackend.marketdata.adapter.out.notification.dto.MarketStatusStompPayload;
import ksh.tryptobackend.marketdata.application.port.out.MarketStatusNotificationPort;
import ksh.tryptobackend.marketdata.domain.vo.MarketStatusNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompMarketStatusNotificationAdapter implements MarketStatusNotificationPort {

    private static final String TOPIC_PREFIX = "/topic/market-status.";

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcast(MarketStatusNotification notification) {
        messagingTemplate.convertAndSend(
                TOPIC_PREFIX + notification.exchangeId(), MarketStatusStompPayload.from(notification));
    }
}
