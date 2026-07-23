package ksh.tryptobackend.marketdata.domain.model;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * 진행 중(아직 닫히지 않은) 캔들을 즉석 집계할 때 쓰는 시간 경계 묶음이다.
 *
 * <p>완성된 하위 구간은 저장된 상위봉으로 채우고 남은 조각만 더 잘게 채우기 위해, 현재 구간의 시작({@code periodStart})과
 * 그 아래 단계 경계(일·시·분 시작)를 함께 계산해 둔다. 일·주·월·4시간 경계는 타임존에 따라 갈리므로 {@code zone} 을
 * 받아 그 기준으로 자른다. 1분·1시간 경계는 타임존과 무관하지만 일관성을 위해 같은 기준으로 계산한다.
 */
public record CandleWindow(Instant periodStart, Instant dayStart, Instant hourStart, Instant minuteStart) {

    public static CandleWindow of(CandleInterval interval, Instant now, ZoneId zone) {
        ZonedDateTime nowZoned = now.atZone(zone);
        Instant minuteStart = nowZoned.truncatedTo(ChronoUnit.MINUTES).toInstant();
        Instant hourStart = nowZoned.truncatedTo(ChronoUnit.HOURS).toInstant();
        Instant dayStart = nowZoned.truncatedTo(ChronoUnit.DAYS).toInstant();
        Instant periodStart = periodStart(interval, nowZoned, dayStart, now);
        return new CandleWindow(periodStart, dayStart, hourStart, minuteStart);
    }

    private static Instant periodStart(CandleInterval interval, ZonedDateTime nowZoned, Instant dayStart, Instant now) {
        return switch (interval) {
            case ONE_MINUTE -> nowZoned.truncatedTo(ChronoUnit.MINUTES).toInstant();
            case ONE_HOUR -> nowZoned.truncatedTo(ChronoUnit.HOURS).toInstant();
            case FOUR_HOURS -> fourHourStart(dayStart, now);
            case ONE_DAY -> dayStart;
            case ONE_WEEK ->
                nowZoned.truncatedTo(ChronoUnit.DAYS)
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .toInstant();
            case ONE_MONTH ->
                nowZoned.truncatedTo(ChronoUnit.DAYS)
                        .with(TemporalAdjusters.firstDayOfMonth())
                        .toInstant();
        };
    }

    // 4시간봉 격자는 그날의 시작을 기준으로 앉힌다. 하루 경계가 정시에 떨어지므로 어떤 타임존이든 완성 1시간봉으로 채울 수 있다.
    private static Instant fourHourStart(Instant dayStart, Instant now) {
        long windowIndex = Duration.between(dayStart, now).toHours() / 4;
        return dayStart.plus(windowIndex * 4, ChronoUnit.HOURS);
    }
}
