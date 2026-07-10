package ksh.tryptobackend.ranking.application.port.out;

import java.time.LocalDate;
import ksh.tryptobackend.ranking.domain.vo.Holdings;
import ksh.tryptobackend.ranking.domain.vo.SnapshotSummaries;

public interface PortfolioQueryPort {

    Holdings findLatestHoldings(Long userId, Long roundId);

    SnapshotSummaries findLatestSummaries(LocalDate snapshotDate);
}
