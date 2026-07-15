package ksh.tryptocollector.backfill;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ksh.tryptocollector.distribute.tick.CandleWriter;
import ksh.tryptocollector.distribute.tick.LastCandleTimeReader;
import ksh.tryptocollector.distribute.tick.TickRawWriter;
import ksh.tryptocollector.ingest.binance.BinanceRestClient;
import ksh.tryptocollector.ingest.bithumb.BithumbRestClient;
import ksh.tryptocollector.ingest.upbit.UpbitRestClient;
import ksh.tryptocollector.model.Candle;
import ksh.tryptocollector.model.Exchange;
import ksh.tryptocollector.model.MarketInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandleBackfillService {

    private static final int UPBIT_PAGE_SIZE = 200;
    private static final int BINANCE_PAGE_SIZE = 1000;
    private static final long OPEN_OFFSET_MS = 0;
    private static final long HIGH_OFFSET_MS = 1;
    private static final long LOW_OFFSET_MS = 2;
    private static final long CLOSE_OFFSET_MS = 3;

    private final UpbitRestClient upbitRestClient;
    private final BithumbRestClient bithumbRestClient;
    private final BinanceRestClient binanceRestClient;
    private final LastCandleTimeReader lastCandleTimeReader;
    private final CandleWriter candleWriter;
    private final TickRawWriter tickRawWriter;

    @Value("${backfill.request-interval-ms:100}")
    private long requestIntervalMs;

    @Value("${backfill.max-pages:10}")
    private int maxPages;

    public void triggerBackfill() {
        ExecutorService pool = Executors.newFixedThreadPool(Exchange.values().length);
        for (Exchange exchange : Exchange.values()) {
            pool.submit(() -> backfillExchangeSafely(exchange));
        }
        pool.shutdown();
    }

    private void backfillExchangeSafely(Exchange exchange) {
        try {
            backfillExchange(exchange);
        } catch (Exception e) {
            log.warn("{} 캔들 백필 실패: {}", exchange, e.getMessage(), e);
        }
    }

    private void backfillExchange(Exchange exchange) {
        List<String> bases = fetchBases(exchange);
        log.info("{} 캔들 백필 시작: 심볼 {}개", exchange, bases.size());
        for (CandleInterval interval : CandleInterval.values()) {
            backfillInterval(exchange, interval, bases);
        }
        log.info("{} 캔들 백필 완료", exchange);
    }

    private void backfillInterval(Exchange exchange, CandleInterval interval, List<String> bases) {
        Map<String, Long> lastTimes =
                lastCandleTimeReader.findLastTimestamps(interval.getMeasurement(), exchange.name());
        for (String base : bases) {
            String symbol = base + "/" + exchange.getQuote();
            Long lastMs = lastTimes.get(symbol);
            if (lastMs == null) {
                continue;
            }
            List<Candle> candles = fetchNewCandles(exchange, interval, base, lastMs);
            writeCandles(interval, candles, lastMs);
        }
    }

    private void writeCandles(CandleInterval interval, List<Candle> candles, long lastMs) {
        if (candles.isEmpty()) {
            return;
        }
        long inProgressStart = candles.stream().mapToLong(Candle::startMs).max().orElseThrow();
        for (Candle candle : candles) {
            if (candle.startMs() <= lastMs) {
                continue;
            }
            if (interval.isMinute() && candle.startMs() == inProgressStart) {
                seedFakeTicks(candle);
            } else {
                candleWriter.write(candle, interval.getMeasurement());
            }
        }
    }

    private void seedFakeTicks(Candle candle) {
        long start = candle.startMs();
        tickRawWriter.writeRawTick(
                candle.exchange(), candle.symbol(), candle.open().doubleValue(), start + OPEN_OFFSET_MS);
        tickRawWriter.writeRawTick(
                candle.exchange(), candle.symbol(), candle.high().doubleValue(), start + HIGH_OFFSET_MS);
        tickRawWriter.writeRawTick(
                candle.exchange(), candle.symbol(), candle.low().doubleValue(), start + LOW_OFFSET_MS);
        tickRawWriter.writeRawTick(
                candle.exchange(), candle.symbol(), candle.close().doubleValue(), start + CLOSE_OFFSET_MS);
    }

    private List<String> fetchBases(Exchange exchange) {
        return switch (exchange) {
            case UPBIT ->
                upbitRestClient.fetchKrwMarkets().stream().map(MarketInfo::base).toList();
            case BITHUMB ->
                bithumbRestClient.fetchKrwMarkets().stream()
                        .map(MarketInfo::base)
                        .toList();
            case BINANCE ->
                binanceRestClient.fetchUsdtTickers().stream()
                        .map(ticker -> ticker.symbol().replace("USDT", ""))
                        .toList();
        };
    }

    private List<Candle> fetchNewCandles(Exchange exchange, CandleInterval interval, String base, long lastMs) {
        if (exchange == Exchange.BINANCE) {
            return fetchBinanceCandles(interval, base, lastMs);
        }
        return fetchUpbitStyleCandles(exchange, interval, base, lastMs);
    }

    private List<Candle> fetchUpbitStyleCandles(Exchange exchange, CandleInterval interval, String base, long lastMs) {
        List<Candle> all = new ArrayList<>();
        String toIso = null;
        for (int page = 0; page < maxPages; page++) {
            List<Candle> pageCandles = exchange == Exchange.UPBIT
                    ? upbitRestClient.fetchCandles(interval.getUpbitPath(), base, toIso, UPBIT_PAGE_SIZE)
                    : bithumbRestClient.fetchCandles(interval.getUpbitPath(), base, toIso, UPBIT_PAGE_SIZE);
            throttle();
            if (pageCandles.isEmpty()) {
                return all;
            }
            all.addAll(pageCandles);
            long oldest = pageCandles.stream().mapToLong(Candle::startMs).min().orElseThrow();
            if (oldest <= lastMs) {
                return all;
            }
            toIso = Instant.ofEpochMilli(oldest).toString();
        }
        log.warn("{} {} {} 백필 최대 페이지({}) 초과 - 다운타임이 너무 깁니다", exchange, interval, base, maxPages);
        return all;
    }

    private List<Candle> fetchBinanceCandles(CandleInterval interval, String base, long lastMs) {
        List<Candle> all = new ArrayList<>();
        Long endTimeMs = null;
        for (int page = 0; page < maxPages; page++) {
            List<Candle> pageCandles =
                    binanceRestClient.fetchCandles(interval.getBinanceInterval(), base, endTimeMs, BINANCE_PAGE_SIZE);
            throttle();
            if (pageCandles.isEmpty()) {
                return all;
            }
            all.addAll(pageCandles);
            long oldest = pageCandles.stream().mapToLong(Candle::startMs).min().orElseThrow();
            if (oldest <= lastMs) {
                return all;
            }
            endTimeMs = oldest - 1;
        }
        log.warn("BINANCE {} {} 백필 최대 페이지({}) 초과 - 다운타임이 너무 깁니다", interval, base, maxPages);
        return all;
    }

    private void throttle() {
        try {
            Thread.sleep(requestIntervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
