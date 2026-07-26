package ksh.tryptobackend.regretanalysis.domain.model;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class ViolationDetails {

    private final List<ViolationDetail> details;

    public ViolationDetails(List<ViolationDetail> details) {
        this.details = List.copyOf(details);
    }

    public Set<Long> extractCoinIds() {
        return details.stream()
                .map(ViolationDetail::getCoinId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Map<Long, List<ViolationDetail>> groupByOrder() {
        return details.stream()
                .filter(ViolationDetail::isOrderViolation)
                .collect(Collectors.groupingBy(ViolationDetail::getOrderId, LinkedHashMap::new, Collectors.toList()));
    }

    public List<ViolationDetail> findMonitoringViolations() {
        return details.stream().filter(ViolationDetail::isMonitoringViolation).toList();
    }

    public List<RuleImpact> toRuleImpacts() {
        return details.stream().collect(Collectors.groupingBy(ViolationDetail::getRuleId)).entrySet().stream()
                .map(entry -> {
                    BigDecimal totalLoss = entry.getValue().stream()
                            .map(ViolationDetail::getLossAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return RuleImpact.create(entry.getKey(), entry.getValue().size(), totalLoss);
                })
                .toList();
    }

    public List<ViolationDetail> toList() {
        return details;
    }
}
