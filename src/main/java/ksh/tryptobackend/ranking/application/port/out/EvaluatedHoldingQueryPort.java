package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.domain.model.EvaluatedHoldings;

public interface EvaluatedHoldingQueryPort {

    EvaluatedHoldings findAllByWalletId(Long walletId, Long exchangeId);
}
