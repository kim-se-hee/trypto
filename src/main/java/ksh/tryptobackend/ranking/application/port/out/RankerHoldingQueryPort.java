package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.domain.vo.RankerHolding;

import java.util.List;

public interface RankerHoldingQueryPort {

    List<RankerHolding> findLatestHoldings(Long userId, Long roundId);
}
