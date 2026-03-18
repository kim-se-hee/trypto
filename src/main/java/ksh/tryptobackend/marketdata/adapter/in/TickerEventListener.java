package ksh.tryptobackend.marketdata.adapter.in;

import ksh.tryptobackend.common.config.RabbitMqConfig;
import ksh.tryptobackend.common.dto.TickerMessage;
import ksh.tryptobackend.marketdata.application.port.in.BroadcastLiveTickerUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TickerEventListener {

    private final BroadcastLiveTickerUseCase broadcastLiveTickerUseCase;

    @RabbitListener(queues = "#{tickerMarketdataQueue.name}", autoStartup = "false", id = RabbitMqConfig.TICKER_MARKETDATA_LISTENER_ID)
    public void onTickerEvent(TickerMessage message) {
        try {
            broadcastLiveTickerUseCase.broadcast(
                message.exchange(), message.symbol(), message.currentPrice(),
                message.changeRate(), message.quoteTurnover(), message.timestamp());
        } catch (Exception e) {
            log.error("시세 브로드캐스트 실패: exchange={}, symbol={}",
                message.exchange(), message.symbol(), e);
        }
    }
}
