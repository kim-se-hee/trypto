package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.application.port.in.PlaceOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.application.port.out.*;
import ksh.tryptobackend.trading.application.port.out.ExchangeCoinPort.ExchangeCoinData;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.trading.domain.vo.TradingVenue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlaceOrderService implements PlaceOrderUseCase {

    private final OrderPersistencePort orderPersistencePort;
    private final WalletBalancePort walletBalancePort;
    private final LivePricePort livePricePort;
    private final TradingVenuePort tradingVenuePort;
    private final ExchangeCoinPort exchangeCoinPort;
    private final Clock clock;

    @Override
    @Transactional
    public Order placeOrder(PlaceOrderCommand command) {
        return orderPersistencePort.findByIdempotencyKey(command.idempotencyKey())
            .orElseGet(() -> createOrder(command));
    }

    private Order createOrder(PlaceOrderCommand command) {
        ExchangeCoinData exchangeCoin = exchangeCoinPort.findById(command.exchangeCoinId())
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));

        TradingVenue venue = tradingVenuePort.findByExchangeId(exchangeCoin.exchangeId())
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now(clock);

        if (command.orderType() == OrderType.MARKET) {
            return placeMarketOrder(command, exchangeCoin, venue, now);
        }
        return placeLimitOrder(command, exchangeCoin, venue, now);
    }

    private Order placeMarketOrder(PlaceOrderCommand command, ExchangeCoinData exchangeCoin,
                                   TradingVenue venue, LocalDateTime now) {
        BigDecimal currentPrice = livePricePort.getCurrentPrice(command.exchangeCoinId());

        if (command.side() == Side.BUY) {
            return placeMarketBuyOrder(command, exchangeCoin, venue, currentPrice, now);
        }
        return placeMarketSellOrder(command, exchangeCoin, venue, currentPrice, now);
    }

    private Order placeMarketBuyOrder(PlaceOrderCommand command, ExchangeCoinData exchangeCoin,
                                      TradingVenue venue, BigDecimal currentPrice, LocalDateTime now) {
        Order order = Order.createMarketBuyOrder(
            command.idempotencyKey(), command.walletId(), command.exchangeCoinId(),
            command.amount(), currentPrice, venue, now);

        BigDecimal available = walletBalancePort.getAvailableBalance(
            command.walletId(), venue.baseCurrencyCoinId());
        if (order.getTotalCostForBuy().compareTo(available) > 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        walletBalancePort.deductBalance(command.walletId(), venue.baseCurrencyCoinId(), order.getTotalCostForBuy());
        walletBalancePort.addBalance(command.walletId(), exchangeCoin.coinId(), order.getQuantity().value());

        return orderPersistencePort.save(order);
    }

    private Order placeMarketSellOrder(PlaceOrderCommand command, ExchangeCoinData exchangeCoin,
                                       TradingVenue venue, BigDecimal currentPrice, LocalDateTime now) {
        BigDecimal available = walletBalancePort.getAvailableBalance(
            command.walletId(), exchangeCoin.coinId());
        if (command.amount().compareTo(available) > 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        Order order = Order.createMarketSellOrder(
            command.idempotencyKey(), command.walletId(), command.exchangeCoinId(),
            command.amount(), currentPrice, venue, now);

        walletBalancePort.deductBalance(command.walletId(), exchangeCoin.coinId(), order.getQuantity().value());

        walletBalancePort.addBalance(command.walletId(), venue.baseCurrencyCoinId(),
            order.getFilledAmount().subtract(order.getFee().amount()));

        return orderPersistencePort.save(order);
    }

    private Order placeLimitOrder(PlaceOrderCommand command, ExchangeCoinData exchangeCoin,
                                  TradingVenue venue, LocalDateTime now) {
        if (command.side() == Side.BUY) {
            return placeLimitBuyOrder(command, venue, now);
        }
        return placeLimitSellOrder(command, exchangeCoin, venue, now);
    }

    private Order placeLimitBuyOrder(PlaceOrderCommand command, TradingVenue venue, LocalDateTime now) {
        Order order = Order.createLimitBuyOrder(
            command.idempotencyKey(), command.walletId(), command.exchangeCoinId(),
            command.amount(), command.price(), venue, now);

        BigDecimal available = walletBalancePort.getAvailableBalance(
            command.walletId(), venue.baseCurrencyCoinId());
        if (order.getTotalCostForBuy().compareTo(available) > 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        walletBalancePort.lockBalance(command.walletId(), venue.baseCurrencyCoinId(), order.getTotalCostForBuy());

        return orderPersistencePort.save(order);
    }

    private Order placeLimitSellOrder(PlaceOrderCommand command, ExchangeCoinData exchangeCoin,
                                      TradingVenue venue, LocalDateTime now) {
        Order order = Order.createLimitSellOrder(
            command.idempotencyKey(), command.walletId(), command.exchangeCoinId(),
            command.amount(), command.price(), venue, now);

        BigDecimal available = walletBalancePort.getAvailableBalance(
            command.walletId(), exchangeCoin.coinId());
        if (order.getQuantity().value().compareTo(available) > 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        walletBalancePort.lockBalance(command.walletId(), exchangeCoin.coinId(), order.getQuantity().value());

        return orderPersistencePort.save(order);
    }
}
