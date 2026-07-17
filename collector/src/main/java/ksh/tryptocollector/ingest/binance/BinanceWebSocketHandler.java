package ksh.tryptocollector.ingest.binance;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import ksh.tryptocollector.distribute.TickerSinkProcessor;
import ksh.tryptocollector.ingest.ConnectionLiveness;
import ksh.tryptocollector.ingest.ExchangeTickerStream;
import ksh.tryptocollector.ingest.RestPollingFallback;
import ksh.tryptocollector.ingest.WebSocketLivenessGuard;
import ksh.tryptocollector.metadata.MarketInfoCache;
import ksh.tryptocollector.model.Exchange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class BinanceWebSocketHandler implements ExchangeTickerStream {
    private static final long MAX_BACKOFF_SECONDS = 60;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final MarketInfoCache marketInfoCache;
    private final TickerSinkProcessor tickerSinkProcessor;
    private final RestPollingFallback restPollingFallback;
    private final WebSocketLivenessGuard livenessGuard;
    private final Counter reconnectCounter;

    public BinanceWebSocketHandler(
            ObjectMapper objectMapper,
            MarketInfoCache marketInfoCache,
            TickerSinkProcessor tickerSinkProcessor,
            RestPollingFallback restPollingFallback,
            WebSocketLivenessGuard livenessGuard,
            MeterRegistry registry) {
        this.objectMapper = objectMapper;
        this.marketInfoCache = marketInfoCache;
        this.tickerSinkProcessor = tickerSinkProcessor;
        this.restPollingFallback = restPollingFallback;
        this.livenessGuard = livenessGuard;
        this.reconnectCounter = Counter.builder("websocket.reconnect")
                .tag("exchange", Exchange.BINANCE.name())
                .register(registry);
    }

    @Value("${exchange.binance.ws-url}")
    private String wsUrl;

    @Override
    public void connect() {
        int retryCount = 0;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                CountDownLatch closeLatch = new CountDownLatch(1);
                ConnectionLiveness liveness = new ConnectionLiveness();
                WebSocket ws = httpClient
                        .newWebSocketBuilder()
                        .buildAsync(URI.create(wsUrl), new BinanceListener(closeLatch, liveness))
                        .join();
                log.info("바이낸스 WebSocket 연결 시작");
                restPollingFallback.stop(Exchange.BINANCE);
                retryCount = 0;
                livenessGuard.watch(Exchange.BINANCE, ws, liveness, closeLatch);
            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted() || e instanceof InterruptedException) {
                    log.info("바이낸스 WebSocket 스레드 종료");
                    return;
                }
                reconnectCounter.increment();
                log.warn("바이낸스 WebSocket 연결 끊김, 재연결 시도 #{}", retryCount + 1, e);
                restPollingFallback.start(Exchange.BINANCE);
                backoff(retryCount++);
            }
        }
    }

    private void handleMessage(String payload) {
        try {
            BinanceTickerMessage[] tickers = objectMapper.readValue(payload, BinanceTickerMessage[].class);
            for (BinanceTickerMessage ticker : tickers) {
                marketInfoCache
                        .find(Exchange.BINANCE, ticker.symbol())
                        .ifPresent(meta -> tickerSinkProcessor.process(ticker.toNormalized(meta.displayName())));
            }
        } catch (Exception e) {
            log.debug("바이낸스 메시지 처리 실패: {}", e.getMessage());
        }
    }

    private void backoff(int retryCount) {
        try {
            long delay = Math.min(1L << retryCount, MAX_BACKOFF_SECONDS);
            Thread.sleep(delay * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private class BinanceListener implements WebSocket.Listener {
        private final CountDownLatch closeLatch;
        private final ConnectionLiveness liveness;
        private final StringBuilder textBuffer = new StringBuilder();

        BinanceListener(CountDownLatch closeLatch, ConnectionLiveness liveness) {
            this.closeLatch = closeLatch;
            this.liveness = liveness;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            liveness.recordReceive();
            textBuffer.append(data);
            if (last) {
                String message = textBuffer.toString();
                textBuffer.setLength(0);
                handleMessage(message);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            liveness.recordReceive();
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            liveness.recordReceive();
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.info("바이낸스 WebSocket 종료: statusCode={}, reason={}", statusCode, reason);
            closeLatch.countDown();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("바이낸스 WebSocket 오류", error);
            closeLatch.countDown();
        }
    }
}
