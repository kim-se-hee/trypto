package ksh.tryptobackend.marketdata.domain.model;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CandleIntervalTest {

    @Nested
    @DisplayName("of() 팩토리 메서드")
    class Of {

        @ParameterizedTest(name = "코드 \"{0}\"은 {1}로 변환된다")
        @CsvSource({
            "1m, ONE_MINUTE",
            "1h, ONE_HOUR",
            "4h, FOUR_HOURS",
            "1d, ONE_DAY",
            "1w, ONE_WEEK",
            "1M, ONE_MONTH"
        })
        @DisplayName("유효한 코드로 CandleInterval을 생성한다")
        void of_validCode_returnsCandleInterval(String code, CandleInterval expected) {
            // When
            CandleInterval interval = CandleInterval.of(code);

            // Then
            assertThat(interval).isEqualTo(expected);
        }

        @ParameterizedTest(name = "코드 \"{0}\"은 예외를 발생시킨다")
        @ValueSource(strings = {"2m", "3h", "1D", "1W", "5d", "", "invalid"})
        @DisplayName("유효하지 않은 코드로 생성하면 INVALID_CANDLE_INTERVAL 예외가 발생한다")
        void of_invalidCode_throwsException(String code) {
            assertThatThrownBy(() -> CandleInterval.of(code))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CANDLE_INTERVAL);
        }
    }

    @Nested
    @DisplayName("getMeasurement()")
    class GetMeasurement {

        @ParameterizedTest(name = "{0}의 measurement는 \"{1}\"이다")
        @CsvSource({
            "ONE_MINUTE, candle_1m",
            "ONE_HOUR, candle_1h",
            "FOUR_HOURS, candle_4h",
            "ONE_DAY, candle_1d",
            "ONE_WEEK, candle_1w",
            "ONE_MONTH, candle_1M"
        })
        @DisplayName("각 주기에 대응하는 InfluxDB measurement 이름을 반환한다")
        void getMeasurement_returnsCorrectMeasurement(CandleInterval interval, String expectedMeasurement) {
            assertThat(interval.getMeasurement()).isEqualTo(expectedMeasurement);
        }
    }

    @Nested
    @DisplayName("getDuration()")
    class GetDuration {

        @Test
        @DisplayName("1분봉의 duration은 1분이다")
        void getDuration_oneMinute_returns1Minute() {
            assertThat(CandleInterval.ONE_MINUTE.getDuration()).isEqualTo(Duration.ofMinutes(1));
        }

        @Test
        @DisplayName("1시간봉의 duration은 1시간이다")
        void getDuration_oneHour_returns1Hour() {
            assertThat(CandleInterval.ONE_HOUR.getDuration()).isEqualTo(Duration.ofHours(1));
        }

        @Test
        @DisplayName("4시간봉의 duration은 4시간이다")
        void getDuration_fourHours_returns4Hours() {
            assertThat(CandleInterval.FOUR_HOURS.getDuration()).isEqualTo(Duration.ofHours(4));
        }

        @Test
        @DisplayName("일봉의 duration은 1일이다")
        void getDuration_oneDay_returns1Day() {
            assertThat(CandleInterval.ONE_DAY.getDuration()).isEqualTo(Duration.ofDays(1));
        }

        @Test
        @DisplayName("주봉의 duration은 7일이다")
        void getDuration_oneWeek_returns7Days() {
            assertThat(CandleInterval.ONE_WEEK.getDuration()).isEqualTo(Duration.ofDays(7));
        }

        @Test
        @DisplayName("월봉의 duration은 30일이다")
        void getDuration_oneMonth_returns30Days() {
            assertThat(CandleInterval.ONE_MONTH.getDuration()).isEqualTo(Duration.ofDays(30));
        }
    }
}
