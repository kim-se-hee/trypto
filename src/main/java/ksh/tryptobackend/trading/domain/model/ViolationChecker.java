package ksh.tryptobackend.trading.domain.model;

import java.util.List;
import java.util.Optional;

public class ViolationChecker {

    public static List<RuleViolation> check(List<InvestmentRule> rules, ViolationCheckContext context) {
        return rules.stream()
            .map(rule -> rule.check(context))
            .flatMap(Optional::stream)
            .toList();
    }
}
