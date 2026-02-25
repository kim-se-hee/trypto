package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.application.port.in.CancelOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.CancelOrderCommand;
import ksh.tryptobackend.trading.application.port.out.ExchangeCoinPort;
import ksh.tryptobackend.trading.application.port.out.ExchangeCoinPort.ExchangeCoinData;
import ksh.tryptobackend.trading.application.port.out.OrderPersistencePort;
import ksh.tryptobackend.trading.application.port.out.TradingVenuePort;
import ksh.tryptobackend.trading.application.port.out.WalletBalancePort;
import ksh.tryptobackend.trading.domain.vo.TradingVenue;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.Side;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelOrderService implements CancelOrderUseCase {

    private final OrderPersistencePort orderPersistencePort;
    private final WalletBalancePort walletBalancePort;
    private final TradingVenuePort tradingVenuePort;
    private final ExchangeCoinPort exchangeCoinPort;

    @Override
    @Transactional
    public Order cancelOrder(CancelOrderCommand command) {
        Order order = orderPersistencePort.findById(command.orderId())
            .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (order.isAlreadyCancelled()) {
            return order;
        }

        order.cancel();
        unlockBalance(order);

        return orderPersistencePort.save(order);
    }

    private void unlockBalance(Order order) {
        ExchangeCoinData exchangeCoin = exchangeCoinPort.findById(order.getExchangeCoinId())
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));

        if (order.getSide() == Side.BUY) {
            TradingVenue venue = tradingVenuePort.findByExchangeId(exchangeCoin.exchangeId())
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
            walletBalancePort.unlockBalance(order.getWalletId(), venue.baseCurrencyCoinId(), order.getTotalCostForBuy());
        } else {
            walletBalancePort.unlockBalance(order.getWalletId(), exchangeCoin.coinId(), order.getQuantity().value());
        }
    }
}
