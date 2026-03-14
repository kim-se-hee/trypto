package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.FindActiveHoldingsUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.HoldingInfoResult;
import ksh.tryptobackend.trading.application.port.out.HoldingQueryPort;
import ksh.tryptobackend.trading.domain.model.Holding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindActiveHoldingsService implements FindActiveHoldingsUseCase {

    private final HoldingQueryPort holdingQueryPort;

    @Override
    public List<HoldingInfoResult> findActiveHoldings(Long walletId) {
        return holdingQueryPort.findAllByWalletId(walletId).stream()
            .filter(Holding::isHolding)
            .map(HoldingInfoResult::from)
            .toList();
    }
}
