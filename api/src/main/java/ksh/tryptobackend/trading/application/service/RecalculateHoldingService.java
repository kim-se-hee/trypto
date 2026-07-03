package ksh.tryptobackend.trading.application.service;

import java.util.List;
import ksh.tryptobackend.trading.application.port.in.RecalculateHoldingUseCase;
import ksh.tryptobackend.trading.application.port.out.MarketQueryPort;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.application.port.out.PositionCommandPort;
import ksh.tryptobackend.trading.domain.model.Position;
import ksh.tryptobackend.trading.domain.vo.FilledOrder;
import ksh.tryptobackend.trading.domain.vo.TradingPair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecalculateHoldingService implements RecalculateHoldingUseCase {

    private final OrderQueryPort orderQueryPort;
    private final PositionCommandPort positionCommandPort;
    private final MarketQueryPort marketQueryPort;

    @Override
    @Transactional
    public void recalculate(Long walletId, Long exchangeCoinId) {
        List<FilledOrder> filledOrders =
                orderQueryPort.findFilledByWalletAndExchangeCoin(walletId, exchangeCoinId);
        TradingPair pair = marketQueryPort.findTradingPair(exchangeCoinId);
        Position position =
                positionCommandPort
                        .findByWalletIdAndCoinId(walletId, pair.tradedCoinId())
                        .orElseGet(() -> Position.empty(walletId, pair.tradedCoinId()));
        position.replayFrom(filledOrders);
        positionCommandPort.save(position);
    }
}
