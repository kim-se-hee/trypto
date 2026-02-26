package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.application.port.in.PlaceOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.application.port.out.*;
import ksh.tryptobackend.trading.application.port.out.ExchangeCoinPort.ExchangeCoinData;
import ksh.tryptobackend.trading.application.strategy.OrderPlacementStrategy;
import ksh.tryptobackend.trading.domain.model.Holding;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.model.RuleViolation;
import ksh.tryptobackend.trading.domain.vo.BalanceChange;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.trading.domain.vo.TradingVenue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceOrderService implements PlaceOrderUseCase {

    private final OrderPersistencePort orderPersistencePort;
    private final WalletBalancePort walletBalancePort;
    private final LivePricePort livePricePort;
    private final TradingVenuePort tradingVenuePort;
    private final ExchangeCoinPort exchangeCoinPort;
    private final HoldingPersistencePort holdingPersistencePort;
    private final ViolationCheckService violationCheckService;
    private final ViolationPersistencePort violationPersistencePort;
    private final List<OrderPlacementStrategy> strategies;
    private final Clock clock;

    @Override
    @Transactional
    public Order placeOrder(PlaceOrderCommand command) {
        return orderPersistencePort.findByIdempotencyKey(command.idempotencyKey())
            .orElseGet(() -> executeOrder(command));
    }

    private Order executeOrder(PlaceOrderCommand command) {
        ExchangeCoinData exchangeCoin = exchangeCoinPort.findById(command.exchangeCoinId())
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));

        TradingVenue venue = tradingVenuePort.findByExchangeId(exchangeCoin.exchangeId())
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));

        OrderPlacementStrategy strategy = resolveStrategy(command.orderType(), command.side());

        BigDecimal currentPrice = livePricePort.getCurrentPrice(command.exchangeCoinId());

        LocalDateTime now = LocalDateTime.now(clock);
        Order order = strategy.createOrder(command, venue, currentPrice, now);

        Long balanceCoinId = strategy.resolveBalanceCoinId(venue, exchangeCoin.coinId());
        BigDecimal available = walletBalancePort.getAvailableBalance(command.walletId(), balanceCoinId);
        order.validateSufficientBalance(available);

        List<RuleViolation> violations = violationCheckService.checkOrderViolations(
            order, command.walletId(), command.exchangeCoinId(), exchangeCoin.coinId(), currentPrice);

        for (BalanceChange change : strategy.planBalanceChanges(order, venue, exchangeCoin.coinId())) {
            applyBalanceChange(command.walletId(), change);
        }

        Order savedOrder = orderPersistencePort.save(order);

        if (order.isMarketOrder()) {
            updateHolding(command.walletId(), exchangeCoin.coinId(), order, currentPrice);
        }

        if (!violations.isEmpty()) {
            violationPersistencePort.saveAll(savedOrder.getId(), violations);
        }

        return savedOrder;
    }

    private void updateHolding(Long walletId, Long coinId, Order order, BigDecimal currentPrice) {
        Holding holding = holdingPersistencePort.findByWalletIdAndCoinId(walletId, coinId)
            .orElseGet(() -> Holding.empty(walletId, coinId));
        if (order.getSide() == Side.BUY) {
            holding.applyBuy(order.getFilledPrice(), order.getQuantity().value(), currentPrice);
        } else {
            holding.applySell(order.getQuantity().value());
        }
        holdingPersistencePort.save(holding);
    }

    private void applyBalanceChange(Long walletId, BalanceChange change) {
        switch (change) {
            case BalanceChange.Deduct d -> walletBalancePort.deductBalance(walletId, d.coinId(), d.amount());
            case BalanceChange.Add a -> walletBalancePort.addBalance(walletId, a.coinId(), a.amount());
            case BalanceChange.Lock l -> walletBalancePort.lockBalance(walletId, l.coinId(), l.amount());
        }
    }

    private OrderPlacementStrategy resolveStrategy(OrderType orderType, Side side) {
        return strategies.stream()
            .filter(s -> s.supports(orderType, side))
            .findFirst()
            .orElseThrow();
    }
}
