package ksh.tryptobackend.common.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StompChannelMetricsConfig {

    private static final String BROKER_PREFIX = "stomp.broker";
    private static final String BROKER_BEAN = "brokerChannelExecutor";
    private static final String CLIENT_INBOUND_PREFIX = "stomp.clientInbound";
    private static final String CLIENT_INBOUND_BEAN = "clientInboundChannelExecutor";
    private static final String CLIENT_OUTBOUND_PREFIX = "stomp.clientOutbound";
    private static final String CLIENT_OUTBOUND_BEAN = "clientOutboundChannelExecutor";

    private final MeterRegistry meterRegistry;
    private final ApplicationContext applicationContext;

    @PostConstruct
    void register() {
        bindIfPresent(BROKER_PREFIX, BROKER_BEAN);
        bindIfPresent(CLIENT_INBOUND_PREFIX, CLIENT_INBOUND_BEAN);
        bindIfPresent(CLIENT_OUTBOUND_PREFIX, CLIENT_OUTBOUND_BEAN);
    }

    private void bindIfPresent(String prefix, String beanName) {
        if (!applicationContext.containsBean(beanName)) {
            log.warn("STOMP {} 채널 executor bean({}) 없음 — 메트릭 등록 생략", prefix, beanName);
            return;
        }
        Object bean = applicationContext.getBean(beanName);
        if (!(bean instanceof ThreadPoolTaskExecutor tpe)) {
            log.warn(
                    "STOMP {} executor 가 ThreadPoolTaskExecutor 가 아님: {} — 큐 메트릭 등록 생략",
                    prefix,
                    bean.getClass().getName());
            return;
        }
        Gauge.builder(prefix + ".queue.size", tpe, e -> e.getThreadPoolExecutor().getQueue().size())
                .description("STOMP 채널 executor 의 대기 큐 크기")
                .register(meterRegistry);
        Gauge.builder(prefix + ".active", tpe, ThreadPoolTaskExecutor::getActiveCount)
                .description("STOMP 채널 executor 의 실행 중 작업 수")
                .register(meterRegistry);
        Gauge.builder(prefix + ".pool.size", tpe, ThreadPoolTaskExecutor::getPoolSize)
                .description("STOMP 채널 executor 의 현재 풀 크기")
                .register(meterRegistry);
        bindRejectedCounter(prefix, tpe);
    }

    private void bindRejectedCounter(String prefix, ThreadPoolTaskExecutor tpe) {
        Counter rejected =
                Counter.builder(prefix + ".rejected")
                        .description("큐가 가득 차서 폐기된 STOMP 채널 작업 수 (메시지 손실 직접 증거)")
                        .register(meterRegistry);
        ThreadPoolExecutor executor = tpe.getThreadPoolExecutor();
        RejectedExecutionHandler delegate = executor.getRejectedExecutionHandler();
        executor.setRejectedExecutionHandler(
                (r, e) -> {
                    rejected.increment();
                    delegate.rejectedExecution(r, e);
                });
    }
}
