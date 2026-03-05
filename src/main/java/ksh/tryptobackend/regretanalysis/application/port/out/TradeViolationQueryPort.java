package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.domain.model.TradeViolation;

import java.util.List;

public interface TradeViolationQueryPort {

    List<TradeViolation> findByRoundIdAndExchangeId(Long roundId, Long exchangeId);
}
