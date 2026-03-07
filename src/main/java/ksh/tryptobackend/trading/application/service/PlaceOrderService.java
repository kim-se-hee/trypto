package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.application.port.in.PlaceOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.application.port.out.*;
import ksh.tryptobackend.trading.application.strategy.OrderPlacementStrategy;
import ksh.tryptobackend.trading.domain.model.Holding;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.model.RuleViolation;
import ksh.tryptobackend.trading.domain.vo.BalanceChange;
import ksh.tryptobackend.trading.domain.vo.ListedCoinRef;
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
    private final ListedCoinPort listedCoinPort;
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
        ListedCoinRef listedCoin = getListedCoin(command.exchangeCoinId());
        TradingVenue venue = getTradingVenue(listedCoin.exchangeId());
        OrderPlacementStrategy strategy = resolveStrategy(command.orderType(), command.side());
        BigDecimal currentPrice = livePricePort.getCurrentPrice(command.exchangeCoinId());

        Order order = strategy.createOrder(command, venue, currentPrice, LocalDateTime.now(clock));
        validateBalance(strategy, order, command.walletId(), venue, listedCoin.coinId());

        List<RuleViolation> violations = violationCheckService.checkOrderViolations(
            order, command.walletId(), command.exchangeCoinId(), listedCoin.coinId(), currentPrice);
        applyBalanceChanges(strategy, order, command.walletId(), venue, listedCoin.coinId());

        Order savedOrder = orderPersistencePort.save(order);
        updateHoldingIfMarketOrder(order, command.walletId(), listedCoin.coinId(), currentPrice);
        saveViolations(savedOrder.getId(), violations);

        return savedOrder;
    }

    private ListedCoinRef getListedCoin(Long exchangeCoinId) {
        return listedCoinPort.findById(exchangeCoinId)
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));
    }

    private TradingVenue getTradingVenue(Long exchangeId) {
        return tradingVenuePort.findByExchangeId(exchangeId)
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    private OrderPlacementStrategy resolveStrategy(OrderType orderType, Side side) {
        return strategies.stream()
            .filter(s -> s.supports(orderType, side))
            .findFirst()
            .orElseThrow();
    }

    private void validateBalance(OrderPlacementStrategy strategy, Order order,
                                  Long walletId, TradingVenue venue, Long coinId) {
        Long balanceCoinId = strategy.resolveBalanceCoinId(venue, coinId);
        BigDecimal available = walletBalancePort.getAvailableBalance(walletId, balanceCoinId);
        order.validateSufficientBalance(available);
    }

    private void applyBalanceChanges(OrderPlacementStrategy strategy, Order order,
                                      Long walletId, TradingVenue venue, Long coinId) {
        for (BalanceChange change : strategy.planBalanceChanges(order, venue, coinId)) {
            applyBalanceChange(walletId, change);
        }
    }

    private void applyBalanceChange(Long walletId, BalanceChange change) {
        switch (change) {
            case BalanceChange.Deduct d -> walletBalancePort.deductBalance(walletId, d.coinId(), d.amount());
            case BalanceChange.Add a -> walletBalancePort.addBalance(walletId, a.coinId(), a.amount());
            case BalanceChange.Lock l -> walletBalancePort.lockBalance(walletId, l.coinId(), l.amount());
        }
    }

    private void updateHoldingIfMarketOrder(Order order, Long walletId, Long coinId,
                                             BigDecimal currentPrice) {
        if (!order.isMarketOrder()) {
            return;
        }
        Holding holding = holdingPersistencePort.findByWalletIdAndCoinId(walletId, coinId)
            .orElseGet(() -> Holding.empty(walletId, coinId));
        if (order.getSide() == Side.BUY) {
            holding.applyBuy(order.getFilledPrice(), order.getQuantity().value(), currentPrice);
        } else {
            holding.applySell(order.getQuantity().value());
        }
        holdingPersistencePort.save(holding);
    }

    private void saveViolations(Long orderId, List<RuleViolation> violations) {
        if (!violations.isEmpty()) {
            violationPersistencePort.saveAll(orderId, violations);
        }
    }
}
