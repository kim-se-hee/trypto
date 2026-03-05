package ksh.tryptobackend.ranking.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RankingPeriod {
    DAILY(1),
    WEEKLY(7),
    MONTHLY(30);

    private final int windowDays;
}
