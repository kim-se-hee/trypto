package ksh.tryptobackend.trading.application.service;

import java.util.List;
import ksh.tryptobackend.trading.application.port.in.RecalculateHoldingUseCase;
import ksh.tryptobackend.trading.application.port.out.HoldingCommandPort;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.domain.model.Holding;
import ksh.tryptobackend.trading.domain.vo.FilledOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecalculateHoldingService implements RecalculateHoldingUseCase {

    private final OrderQueryPort orderQueryPort;
    private final HoldingCommandPort holdingCommandPort;

    @Override
    @Transactional
    public void recalculate(Long walletId, Long coinId) {
        List<FilledOrder> filledOrders = orderQueryPort.findFilledByWalletAndCoin(walletId, coinId);
        Holding holding =
                holdingCommandPort
                        .findByWalletIdAndCoinId(walletId, coinId)
                        .orElseGet(() -> Holding.empty(walletId, coinId));
        holding.replayFrom(filledOrders);
        holdingCommandPort.save(holding);
    }
}
