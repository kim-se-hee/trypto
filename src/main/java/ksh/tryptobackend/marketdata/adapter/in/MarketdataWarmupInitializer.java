package ksh.tryptobackend.marketdata.adapter.in;

import ksh.tryptobackend.common.config.RabbitMqConfig;
import ksh.tryptobackend.marketdata.application.port.in.WarmupExchangeCoinMappingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketdataWarmupInitializer {

    private final WarmupExchangeCoinMappingUseCase warmupExchangeCoinMappingUseCase;
    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("marketdata 워밍업 시작");

        warmupExchangeCoinMappingUseCase.warmup();
        startTickerListener();

        log.info("marketdata 워밍업 완료");
    }

    private void startTickerListener() {
        rabbitListenerEndpointRegistry.getListenerContainer(RabbitMqConfig.TICKER_MARKETDATA_LISTENER_ID).start();
        log.info("marketdata RabbitMQ 시세 리스너 활성화");
    }
}
