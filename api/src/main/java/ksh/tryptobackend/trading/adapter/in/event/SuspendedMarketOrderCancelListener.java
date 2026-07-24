package ksh.tryptobackend.trading.adapter.in.event;

import ksh.tryptobackend.common.event.MarketSuspendedEvent;
import ksh.tryptobackend.trading.application.port.in.AutoCancelOrdersUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuspendedMarketOrderCancelListener {

    private final AutoCancelOrdersUseCase autoCancelOrdersUseCase;

    @EventListener
    public void onMarketSuspended(MarketSuspendedEvent event) {
        autoCancelOrdersUseCase.autoCancel(event.exchangeCoinId());
    }
}
