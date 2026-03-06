package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.FindActiveHoldingsUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.HoldingInfoResult;
import ksh.tryptobackend.trading.application.port.out.HoldingPersistencePort;
import ksh.tryptobackend.trading.domain.model.Holding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindActiveHoldingsService implements FindActiveHoldingsUseCase {

    private final HoldingPersistencePort holdingPersistencePort;

    @Override
    public List<HoldingInfoResult> findActiveHoldings(Long walletId) {
        return holdingPersistencePort.findAllByWalletId(walletId).stream()
            .filter(Holding::isHolding)
            .map(h -> new HoldingInfoResult(h.getCoinId(), h.getAvgBuyPrice(), h.getTotalQuantity()))
            .toList();
    }
}
