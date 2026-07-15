package ksh.tryptocollector.ingest.binance;

import java.math.BigDecimal;

public record BinanceCandleResponse(long openTime, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {

    private static final int OPEN_TIME_INDEX = 0;
    private static final int OPEN_INDEX = 1;
    private static final int HIGH_INDEX = 2;
    private static final int LOW_INDEX = 3;
    private static final int CLOSE_INDEX = 4;

    public static BinanceCandleResponse from(Object[] row) {
        return new BinanceCandleResponse(
                ((Number) row[OPEN_TIME_INDEX]).longValue(),
                new BigDecimal(row[OPEN_INDEX].toString()),
                new BigDecimal(row[HIGH_INDEX].toString()),
                new BigDecimal(row[LOW_INDEX].toString()),
                new BigDecimal(row[CLOSE_INDEX].toString()));
    }
}
