package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.application.port.in.CancelOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.CancelOrderCommand;
import ksh.tryptobackend.trading.application.port.out.ListedCoinPort;
import ksh.tryptobackend.trading.application.port.out.OrderPersistencePort;
import ksh.tryptobackend.trading.application.port.out.TradingVenuePort;
import ksh.tryptobackend.trading.application.port.out.WalletBalancePort;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.ListedCoinRef;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.trading.domain.vo.TradingVenue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelOrderService implements CancelOrderUseCase {

    private final OrderPersistencePort orderPersistencePort;
    private final WalletBalancePort walletBalancePort;
    private final TradingVenuePort tradingVenuePort;
    private final ListedCoinPort listedCoinPort;

    @Override
    @Transactional
    public Order cancelOrder(CancelOrderCommand command) {
        Order order = getOrder(command.orderId());

        if (order.isAlreadyCancelled()) {
            return order;
        }

        order.cancel();
        unlockBalance(order);

        return orderPersistencePort.save(order);
    }

    private Order getOrder(Long orderId) {
        return orderPersistencePort.findById(orderId)
            .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void unlockBalance(Order order) {
        ListedCoinRef listedCoin = listedCoinPort.findById(order.getExchangeCoinId())
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));

        if (order.getSide() == Side.BUY) {
            TradingVenue venue = tradingVenuePort.findByExchangeId(listedCoin.exchangeId())
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
            walletBalancePort.unlockBalance(order.getWalletId(), venue.baseCurrencyCoinId(), order.getSettlementDebit());
        } else {
            walletBalancePort.unlockBalance(order.getWalletId(), listedCoin.coinId(), order.getQuantity().value());
        }
    }
}
