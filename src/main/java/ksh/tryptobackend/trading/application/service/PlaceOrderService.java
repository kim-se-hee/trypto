package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.CheckRuleViolationsUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.CheckRuleViolationsQuery;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.trading.application.port.in.PlaceOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.application.port.out.HoldingCommandPort;
import ksh.tryptobackend.marketdata.application.port.in.GetLivePriceUseCase;
import ksh.tryptobackend.marketdata.application.port.in.GetPriceChangeRateUseCase;
import ksh.tryptobackend.trading.application.port.out.OrderCommandPort;
import ksh.tryptobackend.trading.domain.model.Holding;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.model.RuleViolation;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinMappingResult;
import ksh.tryptobackend.trading.domain.vo.BalanceChange;
import ksh.tryptobackend.trading.domain.vo.OrderMode;
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
    private final HoldingCommandPort holdingCommandPort;

    private final GetLivePriceUseCase getLivePriceUseCase;
    private final GetPriceChangeRateUseCase getPriceChangeRateUseCase;
    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final FindExchangeCoinMappingUseCase findExchangeCoinMappingUseCase;

    private final GetAvailableBalanceUseCase getAvailableBalanceUseCase;
    private final ManageWalletBalanceUseCase manageWalletBalanceUseCase;

    private final CheckRuleViolationsUseCase checkRuleViolationsUseCase;

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
        OrderMode mode = OrderMode.of(command.orderType(), command.side());
        BigDecimal currentPrice = getLivePriceUseCase.getCurrentPrice(command.exchangeCoinId());

        Order order = Order.create(command.orderType(), command.side(),
            command.idempotencyKey(), command.walletId(), command.exchangeCoinId(),
            command.amount(), command.price(), venue, currentPrice, LocalDateTime.now(clock));
        validateBalance(mode, order, command, venue, mapping);

        List<RuleViolation> violations = checkOrderViolations(order, command, mapping, currentPrice);
        order.addViolations(violations);

        applyBalanceChanges(mode, order, command, venue, mapping);

        Order savedOrder = orderCommandPort.save(order);
        updateHoldingIfMarketOrder(order, command, mapping, currentPrice);

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

    private void validateBalance(OrderMode mode, Order order,
                                  PlaceOrderCommand command, TradingVenue venue,
                                  ExchangeCoinMappingResult mapping) {
        Long balanceCoinId = mode.resolveBalanceCoinId(venue, mapping.coinId());
        BigDecimal available = getAvailableBalanceUseCase.getAvailableBalance(command.walletId(), balanceCoinId);
        order.validateSufficientBalance(available);
    }

    private List<RuleViolation> checkOrderViolations(Order order, PlaceOrderCommand command,
                                                      ExchangeCoinMappingResult mapping,
                                                      BigDecimal currentPrice) {
        CheckRuleViolationsQuery query = buildViolationQuery(order, command, mapping, currentPrice);
        return checkRuleViolationsUseCase.checkViolations(query).stream()
            .map(r -> new RuleViolation(r.ruleId(), r.violationReason(), r.createdAt()))
            .toList();
    }

    private CheckRuleViolationsQuery buildViolationQuery(Order order, PlaceOrderCommand command,
                                                          ExchangeCoinMappingResult mapping,
                                                          BigDecimal currentPrice) {
        Holding holding = holdingCommandPort
            .findByWalletIdAndCoinId(command.walletId(), mapping.coinId())
            .orElseGet(() -> Holding.empty(command.walletId(), mapping.coinId()));

        BigDecimal changeRate = getPriceChangeRateUseCase.getChangeRate(command.exchangeCoinId());

        LocalDate today = LocalDate.now(clock);
        long todayOrderCount = orderCommandPort.countByWalletIdAndCreatedAtBetween(
            command.walletId(), today.atStartOfDay(), today.atTime(LocalTime.MAX));

        return new CheckRuleViolationsQuery(
            command.walletId(), order.isBuyOrder(), changeRate,
            holding.getAvgBuyPrice(), holding.getTotalQuantity(), holding.getAveragingDownCount(),
            currentPrice, todayOrderCount, LocalDateTime.now(clock));
    }

    private void applyBalanceChanges(OrderMode mode, Order order,
                                      PlaceOrderCommand command, TradingVenue venue,
                                      ExchangeCoinMappingResult mapping) {
        for (BalanceChange change : mode.planBalanceChanges(order, venue, mapping.coinId())) {
            applyBalanceChange(command.walletId(), change);
        }
    }

    private void applyBalanceChange(Long walletId, BalanceChange change) {
        switch (change) {
            case BalanceChange.Deduct d -> manageWalletBalanceUseCase.deductBalance(walletId, d.coinId(), d.amount());
            case BalanceChange.Add a -> manageWalletBalanceUseCase.addBalance(walletId, a.coinId(), a.amount());
            case BalanceChange.Lock l -> manageWalletBalanceUseCase.lockBalance(walletId, l.coinId(), l.amount());
        }
    }

    private void updateHoldingIfMarketOrder(Order order, PlaceOrderCommand command,
                                             ExchangeCoinMappingResult mapping,
                                             BigDecimal currentPrice) {
        if (!order.isMarketOrder()) {
            return;
        }
        Holding holding = holdingCommandPort.findByWalletIdAndCoinId(command.walletId(), mapping.coinId())
            .orElseGet(() -> Holding.empty(command.walletId(), mapping.coinId()));
        holding.applyOrder(order.getSide(), order.getFilledPrice(), order.getQuantity().value(), currentPrice);
        holdingCommandPort.save(holding);
    }
}
