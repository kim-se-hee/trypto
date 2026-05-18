package ksh.tryptocollector.loadtest;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import ksh.tryptocollector.model.Exchange;

public record RampProfile(List<Phase> phases) {

    public RampProfile {
        Objects.requireNonNull(phases, "phases");
        if (phases.isEmpty()) {
            throw new IllegalArgumentException("phases must not be empty");
        }
    }

    public int rateAt(Exchange exchange, long elapsedMs) {
        long phaseStartMs = 0L;
        Map<Exchange, Integer> previousRates = Map.of();
        for (Phase phase : phases) {
            long phaseEndMs = phaseStartMs + phase.durationMs();
            if (elapsedMs < phaseEndMs) {
                long inPhaseMs = elapsedMs - phaseStartMs;
                double progress = (double) inPhaseMs / phase.durationMs();
                int from = previousRates.getOrDefault(exchange, 0);
                int to = phase.toRates().getOrDefault(exchange, 0);
                return (int) Math.round(from + (to - from) * progress);
            }
            phaseStartMs = phaseEndMs;
            previousRates = phase.toRates();
        }
        return previousRates.getOrDefault(exchange, 0);
    }

    public long totalDurationMs() {
        return phases.stream().mapToLong(Phase::durationMs).sum();
    }

    public record Phase(long durationMs, Map<Exchange, Integer> toRates) {

        public Phase {
            if (durationMs <= 0) {
                throw new IllegalArgumentException("durationMs must be positive");
            }
            Objects.requireNonNull(toRates, "toRates");
        }
    }
}
