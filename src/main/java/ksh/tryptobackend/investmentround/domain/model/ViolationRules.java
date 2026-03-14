package ksh.tryptobackend.investmentround.domain.model;

import java.util.List;
import java.util.Optional;

public record ViolationRules(List<ViolationRule> rules) {

    public List<DetectedViolation> check(ViolationCheckContext context) {
        return rules.stream()
            .map(rule -> rule.check(context))
            .flatMap(Optional::stream)
            .toList();
    }

    public boolean isEmpty() {
        return rules.isEmpty();
    }
}
