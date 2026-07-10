package ksh.tryptobackend.trading.application.support;

import java.util.List;
import ksh.tryptobackend.trading.application.port.in.dto.result.HoldingInfoResult;
import ksh.tryptobackend.trading.application.port.out.PositionQueryPort;
import ksh.tryptobackend.trading.domain.model.Position;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveHoldingReader {

    private final PositionQueryPort positionQueryPort;

    public List<HoldingInfoResult> findActiveHoldings(Long walletId) {
        return positionQueryPort.findAllByWalletId(walletId).stream()
                .filter(Position::isHolding)
                .map(HoldingInfoResult::from)
                .toList();
    }
}
