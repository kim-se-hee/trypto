package ksh.tryptobackend.marketdata.adapter.in;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import ksh.tryptobackend.marketdata.adapter.in.dto.response.TickerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 거래소별 SSE emitter 를 보관하고 같은 byte[] 를 fan-out 한다.
 *
 * <p>STOMP/topic 와 달리 메시지마다 sub-id 같은 사용자별 헤더가 없어 직렬화 결과 한 벌로 N 명에게 동일 frame 을 그대로 write 한다.
 */
@Slf4j
@Component
public class SseTickerEmitterRegistry {

    private static final String ACTIVE_GAUGE = "sse.tickers.emitters.active";
    private static final String E2E_TIMER_NAME = "ticker.collectorToOutbound.duration";
    private static final String SEND_FAILURE_COUNTER = "sse.tickers.send.failure.total";

    private final Map<Long, Set<SseEmitter>> emittersByExchange = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> activeCountByExchange = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final Executor sseOutboundExecutor;
    private final MeterRegistry meterRegistry;
    private final Timer e2eTimer;
    private final Counter sendFailureCounter;

    public SseTickerEmitterRegistry(
            ObjectMapper objectMapper,
            @Qualifier("sseOutboundExecutor") Executor sseOutboundExecutor,
            MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.sseOutboundExecutor = sseOutboundExecutor;
        this.meterRegistry = meterRegistry;
        this.e2eTimer =
                Timer.builder(E2E_TIMER_NAME)
                        .description(
                                "collector 가 ticker 를 publish 한 시각부터 api 가 SSE outbound write 직전까지의"
                                        + " 시간")
                        .publishPercentileHistogram()
                        .register(meterRegistry);
        this.sendFailureCounter =
                Counter.builder(SEND_FAILURE_COUNTER)
                        .description("SSE emitter send 실패 누계 (slow consumer / 연결 끊김 포함)")
                        .register(meterRegistry);
    }

    public void register(long exchangeId, SseEmitter emitter) {
        Set<SseEmitter> targets =
                emittersByExchange.computeIfAbsent(
                        exchangeId, key -> ConcurrentHashMap.newKeySet());
        AtomicInteger counter = activeCounterFor(exchangeId);
        targets.add(emitter);
        counter.incrementAndGet();

        Runnable cleanup =
                () -> {
                    if (targets.remove(emitter)) {
                        counter.decrementAndGet();
                    }
                };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(t -> cleanup.run());
    }

    /**
     * 한 거래소를 구독 중인 모든 emitter 에 같은 payload 를 fan-out 한다. payload 직렬화는 1번만 수행하고 결과 String 인스턴스를
     * emitter 들이 공유한다.
     */
    public void broadcast(long exchangeId, List<TickerResponse> tickers, long publishedAtMs) {
        Set<SseEmitter> targets = emittersByExchange.get(exchangeId);
        if (targets == null || targets.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(tickers);
        } catch (JacksonException e) {
            log.error("ticker batch 직렬화 실패: exchangeId={}", exchangeId, e);
            return;
        }
        long elapsed = System.currentTimeMillis() - publishedAtMs;
        if (elapsed >= 0) {
            e2eTimer.record(elapsed, TimeUnit.MILLISECONDS);
        }
        for (SseEmitter emitter : targets) {
            sseOutboundExecutor.execute(() -> sendQuietly(emitter, json));
        }
    }

    private void sendQuietly(SseEmitter emitter, String json) {
        try {
            emitter.send(SseEmitter.event().data(json));
        } catch (IOException | IllegalStateException e) {
            // slow consumer / 연결 끊김. completeWithError 가 onError 콜백을 호출해 unregister 한다.
            sendFailureCounter.increment();
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
                // 이미 completed 상태일 수 있음
            }
        }
    }

    private AtomicInteger activeCounterFor(long exchangeId) {
        return activeCountByExchange.computeIfAbsent(
                exchangeId,
                key -> {
                    AtomicInteger counter = new AtomicInteger(0);
                    Gauge.builder(ACTIVE_GAUGE, counter, AtomicInteger::get)
                            .description("거래소별 현재 활성 SSE ticker emitter 수")
                            .tag("exchangeId", String.valueOf(key))
                            .register(meterRegistry);
                    return counter;
                });
    }
}
