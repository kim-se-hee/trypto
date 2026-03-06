package ksh.tryptobackend.regretanalysis.domain.vo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public record AnalysisRules(List<AnalysisRule> rules) {

    public boolean isEmpty() {
        return rules.isEmpty();
    }

    public Optional<AnalysisRule> findById(Long ruleId) {
        return rules.stream()
            .filter(rule -> rule.ruleId().equals(ruleId))
            .findFirst();
    }

    public Map<Long, AnalysisRule> toMap() {
        return rules.stream()
            .collect(Collectors.toMap(AnalysisRule::ruleId, r -> r));
    }
}
