package ksh.tryptobackend.trading.adapter.out.service;

import java.util.List;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.CheckRuleViolationsQuery;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RuleViolationCheckResult;
import ksh.tryptobackend.trading.domain.event.OrderPlacedEvent;
import ksh.tryptobackend.trading.domain.model.Position;
import ksh.tryptobackend.trading.domain.model.RuleViolation;
import ksh.tryptobackend.trading.domain.vo.Price;

final class RuleViolationTranslator {

    private RuleViolationTranslator() {}

    static CheckRuleViolationsQuery toQuery(
            OrderPlacedEvent event, Position position, long todayOrderCount) {
        return new CheckRuleViolationsQuery(
                event.walletId(),
                event.exchangeCoinId(),
                event.isBuy(),
                position.isAtLoss(Price.of(event.currentPrice())),
                position.getAveragingDownCount(),
                todayOrderCount,
                event.createdAt());
    }

    static List<RuleViolation> toRuleViolations(List<RuleViolationCheckResult> results) {
        return results.stream()
                .map(r -> RuleViolation.create(r.ruleId(), r.violationReason(), r.createdAt()))
                .toList();
    }
}
