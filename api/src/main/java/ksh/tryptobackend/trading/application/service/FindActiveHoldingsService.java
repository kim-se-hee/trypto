package ksh.tryptobackend.trading.application.service;

import java.util.List;
import ksh.tryptobackend.trading.application.port.in.FindActiveHoldingsUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.HoldingInfoResult;
import ksh.tryptobackend.trading.application.support.ActiveHoldingReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindActiveHoldingsService implements FindActiveHoldingsUseCase {

    private final ActiveHoldingReader activeHoldingReader;

    @Override
    public List<HoldingInfoResult> findActiveHoldings(Long walletId) {
        return activeHoldingReader.findActiveHoldings(walletId);
    }
}
