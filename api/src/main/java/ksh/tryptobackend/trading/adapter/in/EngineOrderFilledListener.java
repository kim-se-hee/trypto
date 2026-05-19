package ksh.tryptobackend.trading.adapter.in;

import ksh.tryptobackend.trading.adapter.in.messages.EngineOrderFilledMessage;
import ksh.tryptobackend.trading.application.port.in.NotifyOrderFilledUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.NotifyOrderFilledCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EngineOrderFilledListener {

    private final NotifyOrderFilledUseCase notifyOrderFilledUseCase;

    @RabbitListener(queues = "#{engineOrderFilledQueue.name}")
    public void onFilled(EngineOrderFilledMessage message) {
        NotifyOrderFilledCommand command =
                new NotifyOrderFilledCommand(
                        message.orderId(),
                        message.userId(),
                        message.executedPrice(),
                        message.quantity(),
                        message.executedAt(),
                        message.matchedAt());
        notifyOrderFilledUseCase.notifyOrderFilled(command);
    }
}
