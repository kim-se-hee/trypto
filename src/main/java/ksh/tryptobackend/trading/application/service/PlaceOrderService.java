package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.application.port.in.PlaceOrderUseCase;
import ksh.tryptobackend.trading.application.port.out.ExchangeCoinPort;
import ksh.tryptobackend.trading.application.port.out.ExchangeCoinPort.ExchangeCoinData;
import ksh.tryptobackend.trading.application.port.out.ExchangePort;
import ksh.tryptobackend.trading.application.port.out.ExchangePort.ExchangeData;
import ksh.tryptobackend.trading.application.port.out.LivePricePort;
import ksh.tryptobackend.trading.application.port.out.OrderPersistencePort;
import ksh.tryptobackend.trading.application.port.out.WalletBalancePort;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Side;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlaceOrderService implements PlaceOrderUseCase {

    private static final Long KRW_COIN_ID = 1L;
    private static final BigDecimal KRW_MIN_ORDER = new BigDecimal("5000");
    private static final BigDecimal KRW_MAX_ORDER = new BigDecimal("1000000000");
    private static final BigDecimal USDT_MIN_ORDER = new BigDecimal("5");

    private final OrderPersistencePort orderPersistencePort;
    private final WalletBalancePort walletBalancePort;
    private final LivePricePort livePricePort;
    private final ExchangePort exchangePort;
    private final ExchangeCoinPort exchangeCoinPort;

    @Override
    @Transactional
    public Order placeOrder(PlaceOrderCommand command) {
        return orderPersistencePort.findByIdempotencyKey(command.idempotencyKey())
                .orElseGet(() -> createOrder(command));
    }

    private Order createOrder(PlaceOrderCommand command) {
        ExchangeCoinData exchangeCoin = exchangeCoinPort.findById(command.exchangeCoinId())
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));

        ExchangeData exchange = exchangePort.findById(exchangeCoin.exchangeId())
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));

        validateOrderAmount(command, exchange.baseCurrencyCoinId());
        validateLimitPrice(command);

        BigDecimal feeRate = exchange.feeRate();
        LocalDateTime now = LocalDateTime.now();

        if (command.orderType() == OrderType.MARKET) {
            return placeMarketOrder(command, exchangeCoin, exchange, feeRate, now);
        }
        return placeLimitOrder(command, exchangeCoin, exchange, feeRate, now);
    }

    private Order placeMarketOrder(PlaceOrderCommand command, ExchangeCoinData exchangeCoin,
                                   ExchangeData exchange, BigDecimal feeRate, LocalDateTime now) {
        BigDecimal currentPrice = livePricePort.getCurrentPrice(command.exchangeCoinId());

        if (command.side() == Side.BUY) {
            return placeMarketBuyOrder(command, exchangeCoin, exchange, currentPrice, feeRate, now);
        }
        return placeMarketSellOrder(command, exchangeCoin, currentPrice, feeRate, now);
    }

    private Order placeMarketBuyOrder(PlaceOrderCommand command, ExchangeCoinData exchangeCoin,
                                      ExchangeData exchange, BigDecimal currentPrice,
                                      BigDecimal feeRate, LocalDateTime now) {
        Order order = Order.createMarketBuyOrder(
                command.idempotencyKey(), command.walletId(), command.exchangeCoinId(),
                command.amount(), currentPrice, feeRate, now);

        BigDecimal available = walletBalancePort.getAvailableBalance(
                command.walletId(), exchange.baseCurrencyCoinId());
        if (order.getTotalCostForBuy().compareTo(available) > 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        walletBalancePort.deductBalance(command.walletId(), exchange.baseCurrencyCoinId(), order.getTotalCostForBuy());
        walletBalancePort.addBalance(command.walletId(), exchangeCoin.coinId(), order.getQuantity());

        return orderPersistencePort.save(order);
    }

    private Order placeMarketSellOrder(PlaceOrderCommand command, ExchangeCoinData exchangeCoin,
                                       BigDecimal currentPrice, BigDecimal feeRate, LocalDateTime now) {
        BigDecimal available = walletBalancePort.getAvailableBalance(
                command.walletId(), exchangeCoin.coinId());
        if (command.amount().compareTo(available) > 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        Order order = Order.createMarketSellOrder(
                command.idempotencyKey(), command.walletId(), command.exchangeCoinId(),
                command.amount(), currentPrice, feeRate, now);

        walletBalancePort.deductBalance(command.walletId(), exchangeCoin.coinId(), order.getQuantity());

        ExchangeData exchange = exchangePort.findById(exchangeCoin.exchangeId())
                .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
        walletBalancePort.addBalance(command.walletId(), exchange.baseCurrencyCoinId(),
                order.getFilledAmount().subtract(order.getFee().getAmount()));

        return orderPersistencePort.save(order);
    }

    private Order placeLimitOrder(PlaceOrderCommand command, ExchangeCoinData exchangeCoin,
                                  ExchangeData exchange, BigDecimal feeRate, LocalDateTime now) {
        if (command.side() == Side.BUY) {
            return placeLimitBuyOrder(command, exchange, feeRate, now);
        }
        return placeLimitSellOrder(command, exchangeCoin, feeRate, now);
    }

    private Order placeLimitBuyOrder(PlaceOrderCommand command, ExchangeData exchange,
                                     BigDecimal feeRate, LocalDateTime now) {
        Order order = Order.createLimitBuyOrder(
                command.idempotencyKey(), command.walletId(), command.exchangeCoinId(),
                command.amount(), command.price(), feeRate, now);

        BigDecimal available = walletBalancePort.getAvailableBalance(
                command.walletId(), exchange.baseCurrencyCoinId());
        if (order.getTotalCostForBuy().compareTo(available) > 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        walletBalancePort.lockBalance(command.walletId(), exchange.baseCurrencyCoinId(), order.getTotalCostForBuy());

        return orderPersistencePort.save(order);
    }

    private Order placeLimitSellOrder(PlaceOrderCommand command, ExchangeCoinData exchangeCoin,
                                      BigDecimal feeRate, LocalDateTime now) {
        Order order = Order.createLimitSellOrder(
                command.idempotencyKey(), command.walletId(), command.exchangeCoinId(),
                command.amount(), command.price(), feeRate, now);

        BigDecimal available = walletBalancePort.getAvailableBalance(
                command.walletId(), exchangeCoin.coinId());
        if (order.getQuantity().compareTo(available) > 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        walletBalancePort.lockBalance(command.walletId(), exchangeCoin.coinId(), order.getQuantity());

        return orderPersistencePort.save(order);
    }

    private void validateOrderAmount(PlaceOrderCommand command, Long baseCurrencyCoinId) {
        BigDecimal amount = command.amount();

        if (command.side() == Side.BUY) {
            if (baseCurrencyCoinId.equals(KRW_COIN_ID)) {
                if (amount.compareTo(KRW_MIN_ORDER) < 0) {
                    throw new CustomException(ErrorCode.BELOW_MIN_ORDER_AMOUNT);
                }
                if (amount.compareTo(KRW_MAX_ORDER) > 0) {
                    throw new CustomException(ErrorCode.ABOVE_MAX_ORDER_AMOUNT);
                }
            } else {
                if (amount.compareTo(USDT_MIN_ORDER) < 0) {
                    throw new CustomException(ErrorCode.BELOW_MIN_ORDER_AMOUNT);
                }
            }
        }
    }

    private void validateLimitPrice(PlaceOrderCommand command) {
        if (command.orderType() == OrderType.LIMIT && command.price() == null) {
            throw new CustomException(ErrorCode.PRICE_REQUIRED_FOR_LIMIT);
        }
    }
}
