package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.GetOrderUseCase;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetOrderService implements GetOrderUseCase {

    private final OrderQueryPort orderQueryPort;

    @Override
    @Transactional(readOnly = true)
    public Order getById(Long orderId) {
        return orderQueryPort.getById(orderId);
    }
}
