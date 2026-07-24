package ksh.tryptobackend.marketdata.adapter.in;

import ksh.tryptobackend.common.config.RabbitMqConfig;
import ksh.tryptobackend.marketdata.adapter.in.dto.MarketStatusChangedMessage;
import ksh.tryptobackend.marketdata.application.port.in.ApplyMarketStatusChangeUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.command.ApplyMarketStatusChangeCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketStatusEventListener {

    private static final String SYMBOL_SEPARATOR = "/";

    private final ApplyMarketStatusChangeUseCase applyMarketStatusChangeUseCase;

    @RabbitListener(
            queues = "#{marketStatusQueue.name}",
            autoStartup = "false",
            id = RabbitMqConfig.MARKET_STATUS_LISTENER_ID)
    public void onMarketStatusChanged(MarketStatusChangedMessage message) {
        try {
            applyMarketStatusChangeUseCase.apply(toCommand(message));
        } catch (Exception e) {
            log.error("상장 상태 변화 반영 실패: {}", message, e);
        }
    }

    private ApplyMarketStatusChangeCommand toCommand(MarketStatusChangedMessage message) {
        String base = message.symbol().split(SYMBOL_SEPARATOR)[0];
        return new ApplyMarketStatusChangeCommand(message.exchange(), base, message.displayName(), message.status());
    }
}
