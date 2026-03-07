package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.domain.model.Ranking;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;

import java.time.LocalDate;
import java.util.List;

public interface RankingCommandPort {

    void replaceByPeriodAndDate(List<Ranking> rankings, RankingPeriod period, LocalDate referenceDate);
}
