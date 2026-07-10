package ksh.tryptobackend.investmentround.domain.model;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.domain.vo.DetectedViolation;
import ksh.tryptobackend.investmentround.domain.vo.RuleEvaluationInput;

public record Rules(List<Rule> rules) {

    public Rules {
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    public static Rules create(List<Rule> rules) {
        List<Rule> source = rules == null ? List.of() : rules;
        EnumSet<RuleType> seenTypes = EnumSet.noneOf(RuleType.class);
        for (Rule rule : source) {
            if (!seenTypes.add(rule.ruleType())) {
                throw new CustomException(ErrorCode.DUPLICATE_RULE_TYPE);
            }
        }
        return new Rules(source);
    }

    public List<DetectedViolation> check(RuleEvaluationInput context) {
        return rules.stream().map(rule -> rule.check(context)).flatMap(Optional::stream).toList();
    }

    public boolean isEmpty() {
        return rules.isEmpty();
    }
}
