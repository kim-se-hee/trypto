package ksh.tryptobackend.trading.application.port.in;

import ksh.tryptobackend.trading.application.port.in.dto.result.EvaluatedHoldingResult;

import java.util.List;

public interface FindEvaluatedHoldingsUseCase {

    List<EvaluatedHoldingResult> findEvaluatedHoldings(Long walletId, Long exchangeId);
}
