package ksh.tryptobackend.trading.application.service;

import java.util.List;
import ksh.tryptobackend.trading.application.port.in.AutoCancelOrdersUseCase;
import ksh.tryptobackend.trading.application.port.out.MarketQueryPort;
import ksh.tryptobackend.trading.application.port.out.OrderCommandPort;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.service.BalanceChangeApplier;
import ksh.tryptobackend.trading.domain.vo.TradingPair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutoCancelOrdersService implements AutoCancelOrdersUseCase {

    private final OrderQueryPort orderQueryPort;
    private final OrderCommandPort orderCommandPort;
    private final MarketQueryPort marketQueryPort;
    private final BalanceChangeApplier balanceChangeApplier;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void autoCancel(Long exchangeCoinId) {
        List<Long> pendingOrderIds = orderQueryPort.findPendingOrderIdsByExchangeCoinId(exchangeCoinId);
        if (pendingOrderIds.isEmpty()) {
            return;
        }
        TradingPair pair = marketQueryPort.getTradingPair(exchangeCoinId);
        for (Long orderId : pendingOrderIds) {
            Order order = orderQueryPort.getByIdWithLock(orderId);
            if (!order.isPending()) {
                continue;
            }
            balanceChangeApplier.applyAll(order.getWalletId(), order.cancel(order.getWalletId(), pair));
            orderCommandPort.save(order);
        }
    }
}
