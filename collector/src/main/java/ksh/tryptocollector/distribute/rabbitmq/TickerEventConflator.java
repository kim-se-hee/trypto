package ksh.tryptocollector.distribute.rabbitmq;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import ksh.tryptocollector.model.NormalizedTicker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * marketdata 채널 전용 conflation + 거래소별 batching.
 * 같은 (exchange, base, quote) 의 tick 이 50ms 윈도우 안에 N건 들어와도 마지막 1건만 살리고,
 * flush 시 거래소별로 묶어 ticker.exchange 로 1 메시지/거래소 발행한다.
 * engine.inbox / Redis / InfluxDB 는 영향 없음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TickerEventConflator {

    static final long FLUSH_INTERVAL_MS = 50L;

    private final TickerEventPublisher publisher;

    private final ConcurrentHashMap<Key, AtomicReference<NormalizedTicker>> slots =
            new ConcurrentHashMap<>();

    public void submit(NormalizedTicker ticker) {
        slots.computeIfAbsent(Key.of(ticker), k -> new AtomicReference<>())
                .set(ticker);
    }

    @Scheduled(fixedDelay = FLUSH_INTERVAL_MS)
    void flush() {
        Map<String, List<NormalizedTicker>> byExchange = new HashMap<>();
        slots.values().forEach(ref -> {
            NormalizedTicker latest = ref.getAndSet(null);
            if (latest != null) {
                byExchange.computeIfAbsent(latest.exchange(), k -> new ArrayList<>()).add(latest);
            }
        });
        byExchange.forEach(publisher::publishBatch);
    }

    @PreDestroy
    void onShutdown() {
        flush();
    }

    private record Key(String exchange, String base, String quote) {
        static Key of(NormalizedTicker t) {
            return new Key(t.exchange(), t.base(), t.quote());
        }
    }
}
