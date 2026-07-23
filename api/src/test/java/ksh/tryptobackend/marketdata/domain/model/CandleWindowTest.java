package ksh.tryptobackend.marketdata.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CandleWindowTest {

    // 2026-07-23T09:26:03Z 은 목요일이다.
    private static final Instant NOW = Instant.parse("2026-07-23T09:26:03Z");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Nested
    @DisplayName("하위 경계(UTC 기준)")
    class SubBoundaries {

        @Test
        @DisplayName("분·시·일 시작을 각각 잘라 담는다")
        void of_truncatesMinuteHourDay() {
            // When
            CandleWindow window = CandleWindow.of(CandleInterval.ONE_DAY, NOW, ZoneOffset.UTC);

            // Then
            assertThat(window.minuteStart()).isEqualTo(Instant.parse("2026-07-23T09:26:00Z"));
            assertThat(window.hourStart()).isEqualTo(Instant.parse("2026-07-23T09:00:00Z"));
            assertThat(window.dayStart()).isEqualTo(Instant.parse("2026-07-23T00:00:00Z"));
        }
    }

    @Nested
    @DisplayName("periodStart — 현재 구간 시작(UTC 기준)")
    class PeriodStartUtc {

        @Test
        @DisplayName("1분봉은 분 시작이다")
        void oneMinute() {
            assertThat(periodStart(CandleInterval.ONE_MINUTE)).isEqualTo(Instant.parse("2026-07-23T09:26:00Z"));
        }

        @Test
        @DisplayName("1시간봉은 시 시작이다")
        void oneHour() {
            assertThat(periodStart(CandleInterval.ONE_HOUR)).isEqualTo(Instant.parse("2026-07-23T09:00:00Z"));
        }

        @Test
        @DisplayName("4시간봉은 그날 기준 4시간 격자에 맞춘다(09:26 → 08:00)")
        void fourHours() {
            assertThat(periodStart(CandleInterval.FOUR_HOURS)).isEqualTo(Instant.parse("2026-07-23T08:00:00Z"));
        }

        @Test
        @DisplayName("일봉은 일 시작이다")
        void oneDay() {
            assertThat(periodStart(CandleInterval.ONE_DAY)).isEqualTo(Instant.parse("2026-07-23T00:00:00Z"));
        }

        @Test
        @DisplayName("주봉은 그 주 월요일 00:00 이다(목요일 → 07-20)")
        void oneWeek() {
            assertThat(periodStart(CandleInterval.ONE_WEEK)).isEqualTo(Instant.parse("2026-07-20T00:00:00Z"));
        }

        @Test
        @DisplayName("월봉은 그 달 1일 00:00 이다")
        void oneMonth() {
            assertThat(periodStart(CandleInterval.ONE_MONTH)).isEqualTo(Instant.parse("2026-07-01T00:00:00Z"));
        }

        private Instant periodStart(CandleInterval interval) {
            return CandleWindow.of(interval, NOW, ZoneOffset.UTC).periodStart();
        }
    }

    @Nested
    @DisplayName("타임존별 경계 분기(Phase 2 대비)")
    class ZoneSpecificBoundaries {

        @Test
        @DisplayName("KST(00:00 KST = 15:00 UTC) 기준이면 일봉 시작이 9시간 밀린다")
        void oneDay_kst_shiftsBy9Hours() {
            // When
            CandleWindow window = CandleWindow.of(CandleInterval.ONE_DAY, NOW, KST);

            // Then — 2026-07-23 18:26 KST 의 하루 시작은 2026-07-23 00:00 KST = 2026-07-22T15:00:00Z
            assertThat(window.periodStart()).isEqualTo(Instant.parse("2026-07-22T15:00:00Z"));
            assertThat(window.dayStart()).isEqualTo(Instant.parse("2026-07-22T15:00:00Z"));
        }

        @Test
        @DisplayName("KST 기준이면 4시간 격자도 KST 자정에 앉는다(16:00 KST = 07:00 UTC)")
        void fourHours_kst_anchoredToKstMidnight() {
            // When
            CandleWindow window = CandleWindow.of(CandleInterval.FOUR_HOURS, NOW, KST);

            // Then
            assertThat(window.periodStart()).isEqualTo(Instant.parse("2026-07-23T07:00:00Z"));
        }
    }
}
