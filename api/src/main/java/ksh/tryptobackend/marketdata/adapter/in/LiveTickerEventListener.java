package ksh.tryptobackend.marketdata.adapter.in;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ksh.tryptobackend.common.config.RabbitMqConfig;
import ksh.tryptobackend.common.dto.messages.TickerBatchMessage;
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
            broadcast(batch);
        } catch (Exception e) {
            log.error(
                    "시세 배치 브로드캐스트 실패: exchange={}, size={}",
                    batch.exchange(),
                    batch.tickers().size(),
                    e);
        }
    }

    private void broadcast(TickerBatchMessage batch) {
        List<TickerResponse> responses = new ArrayList<>(batch.tickers().size());
        Long exchangeId = null;
        long earliestTimestamp = Long.MAX_VALUE;
        for (TickerBatchMessage.Item item : batch.tickers()) {
            LiveTickerResult resolved =
                    resolveLiveTickerUseCase
                            .resolve(
                                    batch.exchange(),
                                    item.symbol(),
                                    item.currentPrice(),
                                    item.changeRate(),
                                    item.quoteTurnover(),
                                    item.timestamp())
                            .orElse(null);
            if (resolved == null) {
                continue;
            }
            if (exchangeId == null) {
                exchangeId = resolved.exchangeId();
            }
            if (resolved.timestamp() != null && resolved.timestamp() < earliestTimestamp) {
                earliestTimestamp = resolved.timestamp();
            }
            responses.add(
                    new TickerResponse(
                            resolved.coinId(),
                            resolved.symbol(),
                            resolved.price(),
                            resolved.changeRate(),
                            resolved.quoteTurnover(),
                            resolved.timestamp()));
        }
        if (responses.isEmpty() || exchangeId == null) {
            return;
        }
        Map<String, Object> headers = Map.of(PUBLISHED_AT_MS_HEADER, earliestTimestamp);
        messagingTemplate.convertAndSend(TOPIC_PREFIX + exchangeId, responses, headers);
    }
}
