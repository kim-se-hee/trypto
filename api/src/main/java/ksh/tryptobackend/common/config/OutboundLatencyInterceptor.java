package ksh.tryptobackend.common.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import ksh.tryptobackend.marketdata.adapter.in.LiveTickerEventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;

public class OutboundLatencyInterceptor implements ExecutorChannelInterceptor {

    private static final String HANDLE_TIMER_NAME = "stomp.clientOutbound.handle.duration";
    private static final String E2E_TIMER_NAME = "ticker.collectorToOutbound.duration";
    private static final String E2E_HEADER_MISSING = "ticker.collectorToOutbound.headerMissing";
    private static final ThreadLocal<Long> START_NANOS = new ThreadLocal<>();

    private final Timer handleTimer;
    private final Timer e2eTimer;
    private final Counter e2eHeaderMissing;

    public OutboundLatencyInterceptor(MeterRegistry registry) {
        this.handleTimer =
                Timer.builder(HANDLE_TIMER_NAME)
                        .description("clientOutbound 채널 핸들러가 메시지 1건을 처리(소켓 write 포함)하는 데 걸린 시간")
                        .publishPercentileHistogram()
                        .register(registry);
        this.e2eTimer =
                Timer.builder(E2E_TIMER_NAME)
                        .description(
                                "collector 가 ticker 를 publish 한 시각부터 api 가 outbound socket write 를 시작하기 직전까지의 시간")
                        .publishPercentileHistogram()
                        .register(registry);
        this.e2eHeaderMissing =
                Counter.builder(E2E_HEADER_MISSING)
                        .description(
                                "outbound 단계에서 publishedAtMs 헤더를 못 찾아 e2e timer 에 기록 못 한 메시지 수. e2e 패널이 비면 이 카운터를 본다.")
                        .register(registry);
    }

    @Override
    public Message<?> beforeHandle(
            Message<?> message, MessageChannel channel, MessageHandler handler) {
        START_NANOS.set(System.nanoTime());
        // SimpMessagingTemplate.convertAndSend(dest, payload, Map) 는 Map 의 값을
        // setNativeHeader(key, value.toString()) 로 변환해 native STOMP header 에 박는다.
        // 그래서 일반 MessageHeaders.get() 으로는 안 잡히고 StompHeaderAccessor 의
        // native header API 로 읽어야 Long 원본을 복원할 수 있다.
        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(message);
        String raw =
                accessor.getFirstNativeHeader(LiveTickerEventListener.PUBLISHED_AT_MS_HEADER);
        if (raw != null) {
            try {
                long ts = Long.parseLong(raw);
                long elapsed = System.currentTimeMillis() - ts;
                if (elapsed >= 0) {
                    e2eTimer.record(elapsed, TimeUnit.MILLISECONDS);
                }
            } catch (NumberFormatException ignored) {
                e2eHeaderMissing.increment();
            }
        } else {
            e2eHeaderMissing.increment();
        }
        return message;
    }

    @Override
    public void afterMessageHandled(
            Message<?> message,
            MessageChannel channel,
            MessageHandler handler,
            Exception ex) {
        Long start = START_NANOS.get();
        if (start == null) {
            return;
        }
        START_NANOS.remove();
        handleTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
    }
}
