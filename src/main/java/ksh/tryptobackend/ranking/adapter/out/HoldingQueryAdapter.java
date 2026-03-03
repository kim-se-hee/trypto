package ksh.tryptobackend.ranking.adapter.out;

import ksh.tryptobackend.ranking.application.port.out.HoldingQueryPort;
import ksh.tryptobackend.ranking.application.port.out.dto.HoldingInfo;
import ksh.tryptobackend.trading.application.port.out.HoldingPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("rankingHoldingQueryAdapter")
@RequiredArgsConstructor
public class HoldingQueryAdapter implements HoldingQueryPort {

    private final HoldingPersistencePort holdingPersistencePort;

    @Override
    public List<HoldingInfo> findAllByWalletId(Long walletId) {
        return holdingPersistencePort.findAllByWalletId(walletId).stream()
            .filter(h -> h.getTotalQuantity().signum() > 0)
            .map(h -> new HoldingInfo(h.getCoinId(), h.getAvgBuyPrice(), h.getTotalQuantity()))
            .toList();
    }
}
