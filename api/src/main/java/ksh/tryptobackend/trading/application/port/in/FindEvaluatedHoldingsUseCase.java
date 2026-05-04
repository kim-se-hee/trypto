package ksh.tryptobackend.trading.application.port.in;

import java.util.List;
import ksh.tryptobackend.trading.application.port.in.dto.result.EvaluatedHoldingResult;

public interface FindEvaluatedHoldingsUseCase {

    List<EvaluatedHoldingResult> findEvaluatedHoldings(Long walletId, Long exchangeId);
}
