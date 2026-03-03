package ksh.tryptobackend.ranking.adapter.out;

import ksh.tryptobackend.ranking.application.port.out.ExchangeCoinQueryPort;
import ksh.tryptobackend.ranking.application.port.out.HoldingQueryPort;
import ksh.tryptobackend.ranking.application.port.out.LivePricePort;
import ksh.tryptobackend.ranking.domain.model.EvaluatedHolding;
import ksh.tryptobackend.ranking.domain.model.EvaluatedHoldings;
import ksh.tryptobackend.trading.application.port.out.HoldingPersistencePort;
import ksh.tryptobackend.trading.domain.model.Holding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component("rankingHoldingQueryAdapter")
@RequiredArgsConstructor
public class HoldingQueryAdapter implements HoldingQueryPort {

    private final HoldingPersistencePort holdingPersistencePort;
    private final ExchangeCoinQueryPort exchangeCoinQueryPort;
    private final LivePricePort livePricePort;

    @Override
    public EvaluatedHoldings findAllByWalletId(Long walletId, Long exchangeId) {
        List<EvaluatedHolding> holdings = holdingPersistencePort.findAllByWalletId(walletId).stream()
            .filter(Holding::isHolding)
            .map(h -> toEvaluatedHolding(h, exchangeId))
            .toList();
        return new EvaluatedHoldings(holdings);
    }

    private EvaluatedHolding toEvaluatedHolding(Holding holding, Long exchangeId) {
        BigDecimal currentPrice = resolveCurrentPrice(exchangeId, holding.getCoinId());
        return EvaluatedHolding.create(
            holding.getCoinId(), holding.getAvgBuyPrice(),
            holding.getTotalQuantity(), currentPrice);
    }

    private BigDecimal resolveCurrentPrice(Long exchangeId, Long coinId) {
        return exchangeCoinQueryPort.findExchangeCoinId(exchangeId, coinId)
            .map(livePricePort::getCurrentPrice)
            .orElse(BigDecimal.ZERO);
    }
}
