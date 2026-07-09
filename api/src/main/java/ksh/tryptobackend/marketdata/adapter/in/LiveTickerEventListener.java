package ksh.tryptobackend.marketdata.adapter.in;

import java.util.List;
import java.util.Map;
import ksh.tryptobackend.common.config.RabbitMqConfig;
import ksh.tryptobackend.common.dto.messages.TickerBatchMessage;
import ksh.tryptobackend.marketdata.adapter.in.dto.response.TickerResponse;
import ksh.tryptobackend.marketdata.application.port.in.ResolveLiveTickerUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.command.ResolveLiveTickerCommand;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.LiveTickerBatchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveTickerEventListener {

    public static final String PUBLISHED_AT_MS_HEADER = "publishedAtMs";
    private static final String TOPIC_PREFIX = "/topic/tickers.";

    private final ResolveLiveTickerUseCase resolveLiveTickerUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(
            queues = "#{tickerMarketdataQueue.name}",
            autoStartup = "false",
            id = RabbitMqConfig.TICKER_MARKETDATA_LISTENER_ID)
    public void onTickerBatch(TickerBatchMessage batch) {
        if (batch.tickers() == null || batch.tickers().isEmpty()) {
            return;
        }
        try {
            resolveLiveTickerUseCase.resolve(toCommand(batch)).ifPresent(this::broadcast);
        } catch (Exception e) {
            log.error(
                    "시세 배치 브로드캐스트 실패: exchange={}, size={}",
                    batch.exchange(),
                    batch.tickers().size(),
                    e);
        }
    }

    private ResolveLiveTickerCommand toCommand(TickerBatchMessage batch) {
        List<ResolveLiveTickerCommand.ExternalTicker> tickers =
                batch.tickers().stream()
                        .map(
                                item ->
                                        new ResolveLiveTickerCommand.ExternalTicker(
                                                item.symbol(),
                                                item.currentPrice(),
                                                item.changeRate(),
                                                item.quoteTurnover(),
                                                item.timestamp()))
                        .toList();
        return new ResolveLiveTickerCommand(batch.exchange(), tickers);
    }

    private void broadcast(LiveTickerBatchResult batch) {
        List<TickerResponse> responses =
                batch.tickers().stream().map(TickerResponse::from).toList();
        Map<String, Object> headers = Map.of(PUBLISHED_AT_MS_HEADER, batch.earliestTimestamp());
        messagingTemplate.convertAndSend(TOPIC_PREFIX + batch.exchangeId(), responses, headers);
    }
}
