package ksh.tryptocollector.loadtest;

import java.util.List;
import java.util.Map;
import ksh.tryptocollector.model.Exchange;

public record StartRampRequest(List<PhaseRequest> phases) {

    public RampProfile toProfile() {
        if (phases == null || phases.isEmpty()) {
            throw new IllegalArgumentException("phases must not be empty");
        }
        List<RampProfile.Phase> mapped =
                phases.stream()
                        .map(PhaseRequest::toPhase)
                        .toList();
        return new RampProfile(mapped);
    }

    public record PhaseRequest(Long durationSeconds, Map<Exchange, Integer> toRates) {

        public RampProfile.Phase toPhase() {
            if (durationSeconds == null || durationSeconds <= 0) {
                throw new IllegalArgumentException("durationSeconds must be positive");
            }
            if (toRates == null) {
                throw new IllegalArgumentException("toRates must not be null");
            }
            return new RampProfile.Phase(durationSeconds * 1000L, toRates);
        }
    }
}
