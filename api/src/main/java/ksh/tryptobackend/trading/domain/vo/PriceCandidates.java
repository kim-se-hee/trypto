package ksh.tryptobackend.trading.domain.vo;

import java.util.List;
import java.util.Optional;

public record PriceCandidates(List<PriceCandidate> candidates) {

    public Optional<PriceCandidate> findFirstMatching(OrphanOrder orphan) {
        for (PriceCandidate candidate : candidates) {
            if (orphan.matches(candidate.price())) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
}
