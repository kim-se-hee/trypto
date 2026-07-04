package ksh.tryptobackend.trading.application.service;

import java.util.List;
import ksh.tryptobackend.trading.application.port.in.CancelOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.CancelOrderCommand;
import ksh.tryptobackend.trading.application.port.out.MarketQueryPort;
import ksh.tryptobackend.trading.application.port.out.OrderCommandPort;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.service.WalletBalanceService;
import ksh.tryptobackend.trading.domain.vo.BalanceChange;
import ksh.tryptobackend.trading.domain.vo.TradingPair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelOrderService implements CancelOrderUseCase {

    private final OrderQueryPort orderQueryPort;
    private final OrderCommandPort orderCommandPort;
    private final MarketQueryPort marketQueryPort;
    private final WalletBalanceService walletBalanceService;

    @Override
    @Transactional
    public Order cancelOrder(CancelOrderCommand command) {
        Order order = orderQueryPort.getByIdWithLock(command.orderId());

        TradingPair pair = marketQueryPort.getTradingPair(order.getExchangeCoinId());
        List<BalanceChange> refund = order.cancel(command.walletId(), pair);
        walletBalanceService.applyAll(order.getWalletId(), refund);

        return orderCommandPort.save(order);
    }
}
