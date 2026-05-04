package ksh.tryptobackend.ranking.application.port.in.dto.query;

import java.time.LocalDate;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;

public record GetRankingsQuery(
        RankingPeriod period, LocalDate referenceDate, Integer cursorRank, int size) {}
