package ksh.tryptobackend.trading.application.port.in;

import java.util.List;
import ksh.tryptobackend.trading.application.port.in.dto.result.HoldingInfoResult;

public interface FindActiveHoldingsUseCase {

    List<HoldingInfoResult> findActiveHoldings(Long walletId);
}
