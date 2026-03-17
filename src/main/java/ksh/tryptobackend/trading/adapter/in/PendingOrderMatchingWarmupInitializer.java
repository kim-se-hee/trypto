package ksh.tryptobackend.trading.adapter.in;

import ksh.tryptobackend.trading.application.port.in.WarmupPendingOrderMatchingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingOrderMatchingWarmupInitializer {

    private final WarmupPendingOrderMatchingUseCase warmupPendingOrderMatchingUseCase;
    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("미체결 주문 매칭 워밍업 시작");

        warmupPendingOrderMatchingUseCase.warmup();
        startTickerListener();

        log.info("미체결 주문 매칭 워밍업 완료");
    }

    private void startTickerListener() {
        rabbitListenerEndpointRegistry.getListenerContainer("tickerMatchingListener").start();
        log.info("RabbitMQ 시세 리스너 활성화");
    }
}
