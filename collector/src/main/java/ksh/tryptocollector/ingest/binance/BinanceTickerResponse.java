package ksh.tryptocollector.ingest.binance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import ksh.tryptocollector.ingest.NormalizableTicker;
import ksh.tryptocollector.model.Exchange;
import ksh.tryptocollector.model.NormalizedTicker;

public record BinanceTickerResponse(String symbol, String lastPrice, String priceChangePercent, String quoteVolume)
        implements NormalizableTicker {
    private static final int CHANGE_RATE_SCALE = 8;
    private static final BigDecimal PERCENT_DIVISOR = BigDecimal.valueOf(100);

    @Override
    public String code() {
        return symbol;
    }

    @Override
    public NormalizedTicker toNormalized(String displayName) {
        String base = symbol.replace("USDT", "");
        BigDecimal changeRate =
                new BigDecimal(priceChangePercent).divide(PERCENT_DIVISOR, CHANGE_RATE_SCALE, RoundingMode.HALF_UP);
        return new NormalizedTicker(
                Exchange.BINANCE.name(),
                base,
                "USDT",
                displayName,
                new BigDecimal(lastPrice),
                changeRate,
                new BigDecimal(quoteVolume),
                System.currentTimeMillis());
    }
}
