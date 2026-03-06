package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.FindFilledOrdersUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.FilledOrderResult;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.application.port.out.dto.OrderInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FindFilledOrdersService implements FindFilledOrdersUseCase {

    private final OrderQueryPort orderQueryPort;

    @Override
    public List<FilledOrderResult> findByOrderIds(List<Long> orderIds) {
        return orderQueryPort.findFilledByOrderIds(orderIds).stream()
            .map(this::toResult)
            .toList();
    }

    @Override
    public List<FilledOrderResult> findSellOrders(Long walletId, Long exchangeCoinId, LocalDateTime after) {
        return orderQueryPort.findFilledSellOrders(walletId, exchangeCoinId, after).stream()
            .map(this::toResult)
            .toList();
    }

    private FilledOrderResult toResult(OrderInfo info) {
        return new FilledOrderResult(
            info.orderId(), info.walletId(), info.exchangeCoinId(),
            info.side().name(), info.amount(), info.quantity(),
            info.filledPrice(), info.filledAt()
        );
    }
}
