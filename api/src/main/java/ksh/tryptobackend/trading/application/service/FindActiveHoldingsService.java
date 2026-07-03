package ksh.tryptobackend.trading.application.service;

import java.util.List;
import ksh.tryptobackend.trading.application.port.in.FindActiveHoldingsUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.HoldingInfoResult;
import ksh.tryptobackend.trading.application.port.out.PositionQueryPort;
import ksh.tryptobackend.trading.domain.model.Position;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindActiveHoldingsService implements FindActiveHoldingsUseCase {

    private final PositionQueryPort positionQueryPort;

    @Override
    public List<HoldingInfoResult> findActiveHoldings(Long walletId) {
        return positionQueryPort.findAllByWalletId(walletId).stream()
                .filter(Position::isHolding)
                .map(HoldingInfoResult::from)
                .toList();
    }
}
