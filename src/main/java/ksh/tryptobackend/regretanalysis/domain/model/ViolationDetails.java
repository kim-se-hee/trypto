package ksh.tryptobackend.regretanalysis.domain.model;

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
            .collect(Collectors.groupingBy(
                ViolationDetail::getOrderId,
                LinkedHashMap::new,
                Collectors.toList()));
    }

    public List<ViolationDetail> findMonitoringViolations() {
        return details.stream()
            .filter(ViolationDetail::isMonitoringViolation)
            .toList();
    }

    public List<ViolationDetail> toList() {
        return details;
    }

}
