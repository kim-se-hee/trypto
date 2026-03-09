package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.application.port.in.PlaceOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.application.port.out.*;
import ksh.tryptobackend.trading.application.strategy.OrderPlacementStrategy;
import ksh.tryptobackend.trading.domain.model.*;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceOrderService implements PlaceOrderUseCase {

    private final OrderCommandPort orderCommandPort;
    private final TradingBalanceQueryPort tradingBalanceQueryPort;
    private final TradingBalanceCommandPort tradingBalanceCommandPort;
    private final LivePriceQueryPort livePriceQueryPort;
    private final TradingVenueQueryPort tradingVenueQueryPort;
    private final ListedCoinQueryPort listedCoinQueryPort;
    private final HoldingCommandPort holdingCommandPort;
    private final ViolationRuleQueryPort violationRuleQueryPort;
    private final PriceChangeRateQueryPort priceChangeRatePort;
    private final List<OrderPlacementStrategy> strategies;
    private final Clock clock;

    @Override
    @Transactional
    public Order placeOrder(PlaceOrderCommand command) {
        return orderCommandPort.findByIdempotencyKey(command.idempotencyKey())
            .orElseGet(() -> executeOrder(command));
    }

    private Order executeOrder(PlaceOrderCommand command) {
        ListedCoinRef listedCoin = getListedCoin(command.exchangeCoinId());
        TradingVenue venue = getTradingVenue(listedCoin.exchangeId());
        OrderPlacementStrategy strategy = resolveStrategy(command.orderType(), command.side());
        BigDecimal currentPrice = livePriceQueryPort.getCurrentPrice(command.exchangeCoinId());

        Order order = strategy.createOrder(command, venue, currentPrice, LocalDateTime.now(clock));
        validateBalance(strategy, order, command.walletId(), venue, listedCoin.coinId());

        List<RuleViolation> violations = checkOrderViolations(
            order, command.walletId(), command.exchangeCoinId(), listedCoin.coinId(), currentPrice);
        order.addViolations(violations);

        applyBalanceChanges(strategy, order, command.walletId(), venue, listedCoin.coinId());

        Order savedOrder = orderCommandPort.save(order);
        updateHoldingIfMarketOrder(order, command.walletId(), listedCoin.coinId(), currentPrice);

        return savedOrder;
    }

    private ListedCoinRef getListedCoin(Long exchangeCoinId) {
        return listedCoinQueryPort.findById(exchangeCoinId)
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));
    }

    private TradingVenue getTradingVenue(Long exchangeId) {
        return tradingVenueQueryPort.findByExchangeId(exchangeId)
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
        BigDecimal available = tradingBalanceQueryPort.getAvailableBalance(walletId, balanceCoinId);
        order.validateSufficientBalance(available);
    }

    private List<RuleViolation> checkOrderViolations(Order order, Long walletId,
                                                      Long exchangeCoinId, Long coinId,
                                                      BigDecimal currentPrice) {
        List<ViolationRule> rules = violationRuleQueryPort.findByWalletId(walletId);
        if (rules.isEmpty()) {
            return List.of();
        }

        ViolationCheckContext context = buildViolationContext(
            order, walletId, exchangeCoinId, coinId, currentPrice);
        return new ViolationRules(rules).check(context);
    }

    private ViolationCheckContext buildViolationContext(Order order, Long walletId,
                                                        Long exchangeCoinId, Long coinId,
                                                        BigDecimal currentPrice) {
        Holding holding = holdingCommandPort
            .findByWalletIdAndCoinId(walletId, coinId)
            .orElse(null);

        BigDecimal changeRate = order.getSide() == Side.BUY
            ? priceChangeRatePort.getChangeRate(exchangeCoinId)
            : BigDecimal.ZERO;

        LocalDate today = LocalDate.now(clock);
        long todayOrderCount = orderCommandPort.countByWalletIdAndCreatedAtBetween(
            walletId, today.atStartOfDay(), today.atTime(LocalTime.MAX));

        return new ViolationCheckContext(
            order.getSide(), changeRate, holding, currentPrice, todayOrderCount,
            LocalDateTime.now(clock));
    }

    private void applyBalanceChanges(OrderPlacementStrategy strategy, Order order,
                                      Long walletId, TradingVenue venue, Long coinId) {
        for (BalanceChange change : strategy.planBalanceChanges(order, venue, coinId)) {
            applyBalanceChange(walletId, change);
        }
    }

    private void applyBalanceChange(Long walletId, BalanceChange change) {
        switch (change) {
            case BalanceChange.Deduct d -> tradingBalanceCommandPort.deductBalance(walletId, d.coinId(), d.amount());
            case BalanceChange.Add a -> tradingBalanceCommandPort.addBalance(walletId, a.coinId(), a.amount());
            case BalanceChange.Lock l -> tradingBalanceCommandPort.lockBalance(walletId, l.coinId(), l.amount());
        }
    }

    private void updateHoldingIfMarketOrder(Order order, Long walletId, Long coinId,
                                             BigDecimal currentPrice) {
        if (!order.isMarketOrder()) {
            return;
        }
        Holding holding = holdingCommandPort.findByWalletIdAndCoinId(walletId, coinId)
            .orElseGet(() -> Holding.empty(walletId, coinId));
        if (order.getSide() == Side.BUY) {
            holding.applyBuy(order.getFilledPrice(), order.getQuantity().value(), currentPrice);
        } else {
            holding.applySell(order.getQuantity().value());
        }
        holdingCommandPort.save(holding);
    }
}
