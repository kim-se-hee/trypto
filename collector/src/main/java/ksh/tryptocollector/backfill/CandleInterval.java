package ksh.tryptocollector.backfill;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CandleInterval {
    ONE_MINUTE("candle_1m", "/v1/candles/minutes/1", "1m"),
    ONE_HOUR("candle_1h", "/v1/candles/minutes/60", "1h"),
    FOUR_HOUR("candle_4h", "/v1/candles/minutes/240", "4h"),
    ONE_DAY("candle_1d", "/v1/candles/days", "1d"),
    ONE_WEEK("candle_1w", "/v1/candles/weeks", "1w"),
    ONE_MONTH("candle_1M", "/v1/candles/months", "1M");

    private final String measurement;
    private final String upbitPath;
    private final String binanceInterval;

    public boolean isMinute() {
        return this == ONE_MINUTE;
    }
}
