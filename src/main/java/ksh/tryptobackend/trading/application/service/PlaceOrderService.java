package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.CheckRuleViolationsUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.CheckRuleViolationsQuery;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.HoldingState;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.trading.application.port.in.PlaceOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.application.port.out.HoldingCommandPort;
import ksh.tryptobackend.marketdata.application.port.in.GetLivePriceUseCase;
import ksh.tryptobackend.trading.application.port.out.OrderCommandPort;
import ksh.tryptobackend.trading.application.port.out.PriceChangeRateQueryPort;
import ksh.tryptobackend.trading.application.strategy.OrderPlacementStrategies;
import ksh.tryptobackend.trading.application.strategy.OrderPlacementStrategy;
import ksh.tryptobackend.trading.domain.model.Holding;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.model.RuleViolation;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinMappingResult;
import ksh.tryptobackend.trading.domain.vo.BalanceChange;
import ksh.tryptobackend.trading.domain.vo.TradingVenue;
import ksh.tryptobackend.wallet.application.port.in.GetAvailableBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.in.ManageWalletBalanceUseCase;
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
    private final GetAvailableBalanceUseCase getAvailableBalanceUseCase;
    private final ManageWalletBalanceUseCase manageWalletBalanceUseCase;
    private final GetLivePriceUseCase getLivePriceUseCase;
    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final FindExchangeCoinMappingUseCase findExchangeCoinMappingUseCase;
    private final HoldingCommandPort holdingCommandPort;
    private final CheckRuleViolationsUseCase checkRuleViolationsUseCase;
    private final PriceChangeRateQueryPort priceChangeRatePort;
    private final OrderPlacementStrategies strategies;
    private final Clock clock;

    @Override
    @Transactional
    public Order placeOrder(PlaceOrderCommand command) {
        return orderCommandPort.findByIdempotencyKey(command.idempotencyKey())
            .orElseGet(() -> executeOrder(command));
    }

    private Order executeOrder(PlaceOrderCommand command) {
        ExchangeCoinMappingResult mapping = getExchangeCoinMapping(command.exchangeCoinId());
        TradingVenue venue = getTradingVenue(mapping.exchangeId());
        OrderPlacementStrategy strategy = strategies.resolve(command.orderType(), command.side());
        BigDecimal currentPrice = getLivePriceUseCase.getCurrentPrice(command.exchangeCoinId());

        Order order = strategy.createOrder(command, venue, currentPrice, LocalDateTime.now(clock));
        validateBalance(strategy, order, command.walletId(), venue, mapping.coinId());

        List<RuleViolation> violations = checkOrderViolations(
            order, command.walletId(), command.exchangeCoinId(), mapping.coinId(), currentPrice);
        order.addViolations(violations);

        applyBalanceChanges(strategy, order, command.walletId(), venue, mapping.coinId());

        Order savedOrder = orderCommandPort.save(order);
        updateHoldingIfMarketOrder(order, command.walletId(), mapping.coinId(), currentPrice);

        return savedOrder;
    }

    private ExchangeCoinMappingResult getExchangeCoinMapping(Long exchangeCoinId) {
        return findExchangeCoinMappingUseCase.findById(exchangeCoinId)
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND));
    }

    private TradingVenue getTradingVenue(Long exchangeId) {
        return findExchangeDetailUseCase.findExchangeDetail(exchangeId)
            .map(detail -> TradingVenue.of(detail.feeRate(), detail.baseCurrencyCoinId(), detail.domestic()))
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    private void validateBalance(OrderPlacementStrategy strategy, Order order,
                                  Long walletId, TradingVenue venue, Long coinId) {
        Long balanceCoinId = strategy.resolveBalanceCoinId(venue, coinId);
        BigDecimal available = getAvailableBalanceUseCase.getAvailableBalance(walletId, balanceCoinId);
        order.validateSufficientBalance(available);
    }

    private List<RuleViolation> checkOrderViolations(Order order, Long walletId,
                                                      Long exchangeCoinId, Long coinId,
                                                      BigDecimal currentPrice) {
        CheckRuleViolationsQuery query = buildViolationQuery(
            order, walletId, exchangeCoinId, coinId, currentPrice);
        return checkRuleViolationsUseCase.checkViolations(query).stream()
            .map(r -> new RuleViolation(r.ruleId(), r.violationReason(), r.createdAt()))
            .toList();
    }

    private CheckRuleViolationsQuery buildViolationQuery(Order order, Long walletId,
                                                          Long exchangeCoinId, Long coinId,
                                                          BigDecimal currentPrice) {
        Holding holding = holdingCommandPort
            .findByWalletIdAndCoinId(walletId, coinId)
            .orElseGet(() -> Holding.empty(walletId, coinId));

        HoldingState holdingState = new HoldingState(
            holding.getAvgBuyPrice(), holding.getTotalQuantity(), holding.getAveragingDownCount());

        BigDecimal changeRate = priceChangeRatePort.getChangeRate(exchangeCoinId);

        LocalDate today = LocalDate.now(clock);
        long todayOrderCount = orderCommandPort.countByWalletIdAndCreatedAtBetween(
            walletId, today.atStartOfDay(), today.atTime(LocalTime.MAX));

        return new CheckRuleViolationsQuery(
            walletId, order.isBuyOrder(), changeRate,
            holdingState, currentPrice, todayOrderCount, LocalDateTime.now(clock));
    }

    private void applyBalanceChanges(OrderPlacementStrategy strategy, Order order,
                                      Long walletId, TradingVenue venue, Long coinId) {
        for (BalanceChange change : strategy.planBalanceChanges(order, venue, coinId)) {
            applyBalanceChange(walletId, change);
        }
    }

    private void applyBalanceChange(Long walletId, BalanceChange change) {
        switch (change) {
            case BalanceChange.Deduct d -> manageWalletBalanceUseCase.deductBalance(walletId, d.coinId(), d.amount());
            case BalanceChange.Add a -> manageWalletBalanceUseCase.addBalance(walletId, a.coinId(), a.amount());
            case BalanceChange.Lock l -> manageWalletBalanceUseCase.lockBalance(walletId, l.coinId(), l.amount());
        }
    }

    private void updateHoldingIfMarketOrder(Order order, Long walletId, Long coinId,
                                             BigDecimal currentPrice) {
        if (!order.isMarketOrder()) {
            return;
        }
        Holding holding = holdingCommandPort.findByWalletIdAndCoinId(walletId, coinId)
            .orElseGet(() -> Holding.empty(walletId, coinId));
        holding.applyOrder(order.getSide(), order.getFilledPrice(), order.getQuantity().value(), currentPrice);
        holdingCommandPort.save(holding);
    }
}
