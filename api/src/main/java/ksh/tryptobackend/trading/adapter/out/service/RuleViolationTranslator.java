package ksh.tryptobackend.trading.adapter.out.service;

import java.math.BigDecimal;
import java.util.List;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.CheckRuleViolationsQuery;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RuleViolationResult;
import ksh.tryptobackend.trading.domain.event.OrderPlacedEvent;
import ksh.tryptobackend.trading.domain.model.Position;
import ksh.tryptobackend.trading.domain.model.RuleViolation;
import ksh.tryptobackend.trading.domain.vo.Holding;

final class RuleViolationTranslator {

    private RuleViolationTranslator() {}

    static CheckRuleViolationsQuery toQuery(
            OrderPlacedEvent event,
            Position position,
            BigDecimal changeRate,
            long todayOrderCount) {
        Holding holding = position.getHolding();
        return new CheckRuleViolationsQuery(
                event.walletId(),
                event.isBuy(),
                changeRate,
                holding.avgBuyPrice().value(),
                holding.totalQuantity().value(),
                position.getAveragingDownCount(),
                event.currentPrice(),
                todayOrderCount,
                event.createdAt());
    }

    static List<RuleViolation> toRuleViolations(List<RuleViolationResult> results) {
        return results.stream()
                .map(r -> RuleViolation.create(r.ruleId(), r.violationReason(), r.createdAt()))
                .toList();
    }
}
