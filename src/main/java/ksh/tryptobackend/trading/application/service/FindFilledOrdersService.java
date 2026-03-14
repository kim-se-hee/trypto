package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.FindFilledOrdersUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.FilledOrderResult;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
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
            .map(FilledOrderResult::from)
            .toList();
    }

    @Override
    public List<FilledOrderResult> findSellOrders(Long walletId, Long exchangeCoinId, LocalDateTime after) {
        return orderQueryPort.findFilledSellOrders(walletId, exchangeCoinId, after).stream()
            .map(FilledOrderResult::from)
            .toList();
    }
}
