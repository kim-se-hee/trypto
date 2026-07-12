package ksh.tryptobackend.marketdata.application.port.in.dto.result;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record LiveTickerBatchResult(Long exchangeId, long earliestTimestamp, List<LiveTickerResult> tickers) {

    public static Optional<LiveTickerBatchResult> from(List<LiveTickerResult> tickers) {
        if (tickers.isEmpty()) {
            return Optional.empty();
        }
        Long exchangeId = tickers.get(0).exchangeId();
        long earliestTimestamp = tickers.stream()
                .map(LiveTickerResult::timestamp)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .min()
                .orElse(Long.MAX_VALUE);
        return Optional.of(new LiveTickerBatchResult(exchangeId, earliestTimestamp, tickers));
    }
}
