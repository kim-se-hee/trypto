package ksh.tryptobackend.trading.application.port.in;

import ksh.tryptobackend.trading.application.port.in.dto.result.HoldingInfoResult;

import java.util.List;

public interface FindActiveHoldingsUseCase {

    List<HoldingInfoResult> findActiveHoldings(Long walletId);
}
