package ksh.tryptobackend.marketdata.adapter.out;

import ksh.tryptobackend.marketdata.adapter.in.dto.response.LivePriceResponse;
import ksh.tryptobackend.marketdata.application.port.out.LivePriceMessagePort;
import ksh.tryptobackend.marketdata.domain.vo.LiveTicker;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompLivePriceMessageAdapter implements LivePriceMessagePort {

    private static final String TOPIC_PREFIX = "/topic/prices.";

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void send(Long exchangeId, LiveTicker liveTicker) {
        LivePriceResponse response = new LivePriceResponse(
            liveTicker.coinId(), liveTicker.symbol(), liveTicker.price(),
            liveTicker.changeRate(), liveTicker.quoteTurnover(), liveTicker.timestamp());
        messagingTemplate.convertAndSend(TOPIC_PREFIX + exchangeId, response);
    }
}
