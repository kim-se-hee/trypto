package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.application.port.in.CancelOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.CancelOrderCommand;
import ksh.tryptobackend.trading.application.port.out.MarketQueryPort;
import ksh.tryptobackend.trading.application.port.out.OrderCommandPort;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.service.WalletBalanceService;
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
        Order order = getOwnedOrder(command);

        if (order.isAlreadyCancelled()) {
            return order;
        }

        order.cancel();
        TradingPair pair = marketQueryPort.findTradingPair(order.getExchangeCoinId());
        walletBalanceService.apply(order.getWalletId(), order.planCancellationRefund(pair));

        return orderCommandPort.save(order);
    }

    private Order getOwnedOrder(CancelOrderCommand command) {
        Order order = orderQueryPort.getByIdWithLock(command.orderId());
        if (!order.isOwnedBy(command.walletId())) {
            throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }
}
