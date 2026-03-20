package ksh.tryptobackend.marketdata.adapter.in;

import ksh.tryptobackend.common.config.RabbitMqConfig;
import ksh.tryptobackend.common.dto.TickerMessage;
import ksh.tryptobackend.marketdata.adapter.in.dto.response.TickerResponse;
import ksh.tryptobackend.marketdata.application.port.in.ResolveLiveTickerUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.LiveTickerResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveTickerEventListener {

    private static final String TOPIC_PREFIX = "/topic/tickers.";

    private final ResolveLiveTickerUseCase resolveLiveTickerUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "#{tickerMarketdataQueue.name}", autoStartup = "false", id = RabbitMqConfig.TICKER_MARKETDATA_LISTENER_ID)
    public void onTickerEvent(TickerMessage message) {
        try {
            resolveLiveTickerUseCase.resolve(
                    message.exchange(), message.symbol(), message.currentPrice(),
                    message.changeRate(), message.quoteTurnover(), message.timestamp())
                .ifPresent(this::broadcast);
        } catch (Exception e) {
            log.error("시세 브로드캐스트 실패: exchange={}, symbol={}",
                message.exchange(), message.symbol(), e);
        }
    }

    private void broadcast(LiveTickerResult result) {
        TickerResponse response = new TickerResponse(
            result.coinId(), result.symbol(), result.price(),
            result.changeRate(), result.quoteTurnover(), result.timestamp());
        messagingTemplate.convertAndSend(TOPIC_PREFIX + result.exchangeId(), response);
    }
}
