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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("rankingHoldingQueryAdapter")
@RequiredArgsConstructor
public class HoldingQueryAdapter implements HoldingQueryPort {

    private final HoldingPersistencePort holdingPersistencePort;
    private final ExchangeCoinQueryPort exchangeCoinQueryPort;
    private final LivePricePort livePricePort;

    @Override
    public EvaluatedHoldings findAllByWalletId(Long walletId, Long exchangeId) {
        List<Holding> activeHoldings = holdingPersistencePort.findAllByWalletId(walletId).stream()
            .filter(Holding::isHolding)
            .toList();

        if (activeHoldings.isEmpty()) {
            return new EvaluatedHoldings(List.of());
        }

        List<Long> coinIds = activeHoldings.stream().map(Holding::getCoinId).toList();
        Map<Long, Long> coinToExchangeCoinMap = exchangeCoinQueryPort.findExchangeCoinIdsByExchangeIdAndCoinIds(exchangeId, coinIds);
        Map<Long, BigDecimal> priceMap = livePricePort.getCurrentPrices(new ArrayList<>(coinToExchangeCoinMap.values()));

        List<EvaluatedHolding> holdings = activeHoldings.stream()
            .map(h -> toEvaluatedHolding(h, coinToExchangeCoinMap, priceMap))
            .toList();
        return new EvaluatedHoldings(holdings);
    }

    private EvaluatedHolding toEvaluatedHolding(Holding holding, Map<Long, Long> coinToExchangeCoinMap,
                                                 Map<Long, BigDecimal> priceMap) {
        Long exchangeCoinId = coinToExchangeCoinMap.get(holding.getCoinId());
        BigDecimal currentPrice = exchangeCoinId != null
            ? priceMap.getOrDefault(exchangeCoinId, BigDecimal.ZERO)
            : BigDecimal.ZERO;
        return EvaluatedHolding.create(
            holding.getCoinId(), holding.getAvgBuyPrice(),
            holding.getTotalQuantity(), currentPrice);
    }
}
