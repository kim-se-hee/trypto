package ksh.tryptobackend.ranking.application.port.in.dto.query;

import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;

import java.time.LocalDate;

public record GetRankingsQuery(
    RankingPeriod period,
    LocalDate referenceDate,
    Integer cursorRank,
    int size
) {
}
