package ksh.tryptocollector.loadtest;

import java.util.Map;
import ksh.tryptocollector.model.Exchange;

public record GeneratorStatus(boolean active, long elapsedMs, Map<Exchange, Integer> currentRates) {

    public static GeneratorStatus idle() {
        return new GeneratorStatus(false, 0L, Map.of());
    }
}
