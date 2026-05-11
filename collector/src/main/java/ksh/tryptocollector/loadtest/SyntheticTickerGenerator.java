package ksh.tryptocollector.loadtest;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import ksh.tryptocollector.distribute.rabbitmq.TickerEventConflator;
import ksh.tryptocollector.metadata.MarketInfoCache;
import ksh.tryptocollector.model.Exchange;
import ksh.tryptocollector.model.MarketInfo;
import ksh.tryptocollector.model.NormalizedTicker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("loadtest")
@RequiredArgsConstructor
public class SyntheticTickerGenerator {

    private static final long TICK_INTERVAL_MS = 100L;
    private static final double TICK_FRACTION_OF_SECOND = TICK_INTERVAL_MS / 1000.0;
    private static final int CHANGE_RATE_SCALE = 6;
    private static final BigDecimal BASE_PRICE = new BigDecimal("50000.00");
    private static final BigDecimal BASE_TURNOVER = new BigDecimal("1000000000");
    private static final double JITTER_AMPLITUDE = 0.001;
    private static final String THREAD_NAME = "synthetic-ticker";
    private static final String METRIC_NAME = "loadtest.synthetic.ticker.published";

    private final TickerEventConflator conflator;
    private final MarketInfoCache marketInfoCache;
    private final MeterRegistry meterRegistry;

    private final Object lifecycleLock = new Object();
    private final Map<Exchange, AtomicInteger> coinIndexes = new ConcurrentHashMap<>();
    private final Map<Exchange, Double> rateCarry = new ConcurrentHashMap<>();
    private final Map<Exchange, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final Map<Exchange, Counter> publishedCounters = new ConcurrentHashMap<>();

    private volatile ScheduledExecutorService scheduler;
    private volatile RampProfile activeProfile;
    private volatile long profileStartedAtMs;

    public void startRamp(RampProfile profile) {
        synchronized (lifecycleLock) {
            stopInternal();
            activeProfile = profile;
            profileStartedAtMs = System.currentTimeMillis();
            scheduler = Executors.newScheduledThreadPool(Exchange.values().length, this::newThread);
            for (Exchange exchange : Exchange.values()) {
                coinIndexes.put(exchange, new AtomicInteger(0));
                rateCarry.put(exchange, 0.0);
                publishedCounters.computeIfAbsent(exchange, this::newCounter);
                ScheduledFuture<?> task =
                        scheduler.scheduleAtFixedRate(
                                () -> tick(exchange),
                                0L,
                                TICK_INTERVAL_MS,
                                TimeUnit.MILLISECONDS);
                tasks.put(exchange, task);
            }
            log.info(
                    "synthetic ticker ramp 시작: phases={}, totalDurationMs={}",
                    profile.phases().size(),
                    profile.totalDurationMs());
        }
    }

    public void stop() {
        synchronized (lifecycleLock) {
            stopInternal();
            log.info("synthetic ticker 발행 중지");
        }
    }

    public GeneratorStatus status() {
        RampProfile profile = activeProfile;
        if (profile == null) {
            return GeneratorStatus.idle();
        }
        long elapsed = System.currentTimeMillis() - profileStartedAtMs;
        Map<Exchange, Integer> rates = new EnumMap<>(Exchange.class);
        for (Exchange exchange : Exchange.values()) {
            rates.put(exchange, profile.rateAt(exchange, elapsed));
        }
        return new GeneratorStatus(true, elapsed, rates);
    }

    @PreDestroy
    void onShutdown() {
        stop();
    }

    private void tick(Exchange exchange) {
        RampProfile profile = activeProfile;
        if (profile == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - profileStartedAtMs;
        int currentRate = profile.rateAt(exchange, elapsed);
        if (currentRate <= 0) {
            return;
        }
        List<MarketInfo> markets = marketInfoCache.getMarketInfos(exchange);
        if (markets.isEmpty()) {
            return;
        }
        double accumulated = currentRate * TICK_FRACTION_OF_SECOND + rateCarry.get(exchange);
        int toPublish = (int) accumulated;
        rateCarry.put(exchange, accumulated - toPublish);
        if (toPublish == 0) {
            return;
        }
        AtomicInteger index = coinIndexes.get(exchange);
        Counter counter = publishedCounters.get(exchange);
        for (int i = 0; i < toPublish; i++) {
            int idx = Math.floorMod(index.getAndIncrement(), markets.size());
            try {
                conflator.submit(buildTicker(exchange, markets.get(idx)));
                counter.increment();
            } catch (Exception e) {
                log.warn("synthetic ticker 제출 실패: exchange={}", exchange, e);
            }
        }
    }

    private NormalizedTicker buildTicker(Exchange exchange, MarketInfo info) {
        double jitter = (ThreadLocalRandom.current().nextDouble() - 0.5) * JITTER_AMPLITUDE * 2;
        BigDecimal jitterBd = BigDecimal.valueOf(jitter);
        BigDecimal price = BASE_PRICE.add(BASE_PRICE.multiply(jitterBd));
        BigDecimal changeRate = jitterBd.setScale(CHANGE_RATE_SCALE, RoundingMode.HALF_UP);
        return new NormalizedTicker(
                exchange.name(),
                info.base(),
                info.quote(),
                info.displayName(),
                price,
                changeRate,
                BASE_TURNOVER,
                System.currentTimeMillis());
    }

    private void stopInternal() {
        if (scheduler != null) {
            tasks.values().forEach(t -> t.cancel(false));
            tasks.clear();
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }
        coinIndexes.clear();
        rateCarry.clear();
        activeProfile = null;
    }

    private Thread newThread(Runnable r) {
        Thread thread = new Thread(r, THREAD_NAME);
        thread.setDaemon(true);
        return thread;
    }

    private Counter newCounter(Exchange exchange) {
        return Counter.builder(METRIC_NAME)
                .description("부하 테스트용 합성 시세 발행 누계")
                .tag("exchange", exchange.name())
                .register(meterRegistry);
    }
}
