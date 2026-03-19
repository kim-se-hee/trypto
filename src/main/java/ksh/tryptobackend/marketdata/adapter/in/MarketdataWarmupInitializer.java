package ksh.tryptobackend.marketdata.adapter.in;

import ksh.tryptobackend.common.config.RabbitMqConfig;
import ksh.tryptobackend.marketdata.application.port.in.SyncMarketMetaUseCase;
import ksh.tryptobackend.marketdata.application.port.in.WarmupExchangeCoinMappingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketdataWarmupInitializer {

    private final SyncMarketMetaUseCase syncMarketMetaUseCase;
    private final WarmupExchangeCoinMappingUseCase warmupExchangeCoinMappingUseCase;
    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    @Value("${app.market-meta-sync.max-retries:30}")
    private int maxRetries;

    @Value("${app.market-meta-sync.retry-interval-seconds:5}")
    private int retryIntervalSeconds;

    @Order(1)
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("marketdata 초기화 시작");

        waitForMarketMetaSync();
        warmupExchangeCoinMappingUseCase.warmup();
        startTickerListener();

        log.info("marketdata 초기화 완료");
    }

    private void waitForMarketMetaSync() {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (syncMarketMetaUseCase.sync()) {
                return;
            }
            log.info("Redis market-meta 대기 중... ({}/{})", attempt, maxRetries);
            sleep();
        }
        log.warn("market-meta 데이터를 {}회 시도 후에도 찾지 못했습니다. 빈 상태로 기동합니다", maxRetries);
    }

    private void sleep() {
        try {
            Thread.sleep(retryIntervalSeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("market-meta 대기 중 인터럽트 발생");
        }
    }

    private void startTickerListener() {
        rabbitListenerEndpointRegistry.getListenerContainer(RabbitMqConfig.TICKER_MARKETDATA_LISTENER_ID).start();
        log.info("marketdata RabbitMQ 시세 리스너 활성화");
    }
}
