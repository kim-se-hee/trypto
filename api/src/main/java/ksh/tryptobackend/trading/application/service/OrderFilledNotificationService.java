package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.NotifyOrderFilledUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.NotifyOrderFilledCommand;
import ksh.tryptobackend.trading.application.port.out.OrderFilledNotificationPort;
import ksh.tryptobackend.trading.domain.vo.OrderFilledNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderFilledNotificationService implements NotifyOrderFilledUseCase {

    private final OrderFilledNotificationPort orderFilledNotificationPort;

    @Override
    public void notifyOrderFilled(NotifyOrderFilledCommand command) {
        OrderFilledNotification notification =
                new OrderFilledNotification(
                        command.orderId(),
                        command.userId(),
                        command.executedPrice(),
                        command.quantity(),
                        command.executedAt(),
                        command.matchedAt());
        orderFilledNotificationPort.push(notification);
    }
}
