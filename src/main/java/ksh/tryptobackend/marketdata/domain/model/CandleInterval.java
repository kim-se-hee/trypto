package ksh.tryptobackend.marketdata.domain.model;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum CandleInterval {

    ONE_MINUTE("1m", "candle_1m", Duration.ofMinutes(1)),
    ONE_HOUR("1h", "candle_1h", Duration.ofHours(1)),
    FOUR_HOURS("4h", "candle_4h", Duration.ofHours(4)),
    ONE_DAY("1d", "candle_1d", Duration.ofDays(1)),
    ONE_WEEK("1w", "candle_1w", Duration.ofDays(7)),
    ONE_MONTH("1M", "candle_1M", Duration.ofDays(30));

    private final String code;
    private final String measurement;
    private final Duration duration;

    public static CandleInterval of(String code) {
        return Arrays.stream(values())
            .filter(interval -> interval.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CANDLE_INTERVAL));
    }
}
