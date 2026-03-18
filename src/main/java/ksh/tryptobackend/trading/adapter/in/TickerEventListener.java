package ksh.tryptobackend.trading.adapter.in;

import ksh.tryptobackend.common.config.RabbitMqConfig;
import ksh.tryptobackend.common.dto.TickerMessage;
import ksh.tryptobackend.trading.application.port.in.MatchPendingOrdersUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TickerEventListener {

    private final MatchPendingOrdersUseCase matchPendingOrdersUseCase;

    @RabbitListener(queues = "#{tickerTradingQueue.name}", autoStartup = "false", id = RabbitMqConfig.TICKER_TRADING_LISTENER_ID)
    public void onTickerEvent(TickerMessage message) {
        try {
            matchPendingOrdersUseCase.matchOrders(
                message.exchange(), message.symbol(), message.currentPrice());
        } catch (Exception e) {
            log.error("시세 이벤트 처리 실패: exchange={}, symbol={}",
                message.exchange(), message.symbol(), e);
        }
    }
}
