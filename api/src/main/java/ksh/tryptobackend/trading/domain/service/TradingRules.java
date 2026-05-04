package ksh.tryptobackend.trading.domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import ksh.tryptobackend.investmentround.application.port.in.CheckRuleViolationsUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.CheckRuleViolationsQuery;
import ksh.tryptobackend.marketdata.application.port.in.GetPriceChangeRateUseCase;
import ksh.tryptobackend.trading.application.port.out.HoldingQueryPort;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.domain.model.Holding;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.model.RuleViolation;
import ksh.tryptobackend.trading.domain.vo.TradingContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradingRules {

    private final HoldingQueryPort holdingQueryPort;
    private final OrderQueryPort orderQueryPort;

    private final GetPriceChangeRateUseCase getPriceChangeRateUseCase;
    private final CheckRuleViolationsUseCase checkRuleViolationsUseCase;

    private final Clock clock;

    public void inspect(Order order, TradingContext ctx) {
        Holding holding =
                holdingQueryPort
                        .findByWalletIdAndCoinId(order.getWalletId(), ctx.coinId())
                        .orElseGet(() -> Holding.empty(order.getWalletId(), ctx.coinId()));

        BigDecimal changeRate = getPriceChangeRateUseCase.getChangeRate(order.getExchangeCoinId());

        LocalDate today = LocalDate.now(clock);
        long todayOrderCount =
                orderQueryPort.countByWalletIdAndCreatedAtBetween(
                        order.getWalletId(), today.atStartOfDay(), today.atTime(LocalTime.MAX));

        CheckRuleViolationsQuery query =
                new CheckRuleViolationsQuery(
                        order.getWalletId(),
                        order.isBuyOrder(),
                        changeRate,
                        holding.getAvgBuyPrice(),
                        holding.getTotalQuantity(),
                        holding.getAveragingDownCount(),
                        ctx.currentPrice(),
                        todayOrderCount,
                        ctx.now());

        List<RuleViolation> violations =
                checkRuleViolationsUseCase.checkViolations(query).stream()
                        .map(r -> new RuleViolation(r.ruleId(), r.violationReason(), r.createdAt()))
                        .toList();

        order.addViolations(violations);
    }
}
