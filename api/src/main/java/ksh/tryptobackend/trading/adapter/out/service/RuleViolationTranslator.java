package ksh.tryptobackend.trading.adapter.out.service;

import java.util.List;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.CheckRuleViolationsQuery;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RuleViolationCheckResult;
import ksh.tryptobackend.trading.domain.event.OrderPlacedEvent;
import ksh.tryptobackend.trading.domain.model.RuleViolation;

final class RuleViolationTranslator {

    private RuleViolationTranslator() {}

    static CheckRuleViolationsQuery toQuery(OrderPlacedEvent event, long todayOrderCount) {
        return new CheckRuleViolationsQuery(
                event.walletId(),
                event.exchangeCoinId(),
                event.isBuy(),
                event.atLoss(),
                event.averagingDownCount(),
                todayOrderCount,
                event.createdAt());
    }

    static List<RuleViolation> toRuleViolations(List<RuleViolationCheckResult> results) {
        return results.stream()
                .map(r -> RuleViolation.create(r.ruleId(), r.violationReason(), r.createdAt()))
                .toList();
    }
}
