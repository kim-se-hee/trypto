package ksh.tryptobackend.regretanalysis.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;
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

    public List<ViolationDetail> toList() {
        return details;
    }

}
