package ksh.tryptobackend.trading.adapter.in.messaging;

import ksh.tryptobackend.trading.application.port.in.NotifyFilledOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.NotifyOrderFilledCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EngineOrderFilledListener {

    private final NotifyFilledOrderUseCase notifyFilledOrderUseCase;

    @RabbitListener(queues = "#{engineOrderFilledQueue.name}")
    public void onFilled(OrderFilledEngineMessage message) {
        NotifyOrderFilledCommand command =
                new NotifyOrderFilledCommand(
                        message.orderId(),
                        message.executedPrice(),
                        message.quantity(),
                        message.executedAt(),
                        message.matchedAt());
        notifyFilledOrderUseCase.notifyOrderFilled(command);
    }
}
