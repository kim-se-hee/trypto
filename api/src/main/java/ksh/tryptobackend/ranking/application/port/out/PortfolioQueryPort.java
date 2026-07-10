package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.domain.vo.Holdings;

public interface PortfolioQueryPort {

    Holdings findLatestHoldings(Long userId, Long roundId);
}
