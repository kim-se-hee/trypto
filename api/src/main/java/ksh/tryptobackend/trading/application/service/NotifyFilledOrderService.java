package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.NotifyFilledOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.NotifyOrderFilledCommand;
import ksh.tryptobackend.trading.application.port.out.OrderFilledNotificationPort;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.application.port.out.WalletQueryPort;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.OrderFilledNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotifyFilledOrderService implements NotifyFilledOrderUseCase {

    private final OrderQueryPort orderQueryPort;
    private final WalletQueryPort walletQueryPort;
    private final OrderFilledNotificationPort orderFilledNotificationPort;

    @Override
    public void notifyOrderFilled(NotifyOrderFilledCommand command) {
        Order order = orderQueryPort.getById(command.orderId());
        Long userId = walletQueryPort.getOwnerId(order.getWalletId());

        OrderFilledNotification notification = new OrderFilledNotification(
                command.orderId(),
                userId,
                command.executedPrice(),
                command.quantity(),
                command.executedAt(),
                command.matchedAt());
        orderFilledNotificationPort.push(notification);
    }
}
