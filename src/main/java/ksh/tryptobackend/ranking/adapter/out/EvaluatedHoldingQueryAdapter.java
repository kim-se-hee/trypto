package ksh.tryptobackend.ranking.adapter.out;

import ksh.tryptobackend.ranking.application.port.out.ExchangeCoinQueryPort;
import ksh.tryptobackend.ranking.application.port.out.EvaluatedHoldingQueryPort;
import ksh.tryptobackend.ranking.application.port.out.LivePricePort;
import ksh.tryptobackend.ranking.domain.model.EvaluatedHolding;
import ksh.tryptobackend.ranking.domain.model.EvaluatedHoldings;
import ksh.tryptobackend.trading.application.port.in.FindActiveHoldingsUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.HoldingInfoResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("rankingEvaluatedHoldingQueryAdapter")
@RequiredArgsConstructor
public class EvaluatedHoldingQueryAdapter implements EvaluatedHoldingQueryPort {

    private final FindActiveHoldingsUseCase findActiveHoldingsUseCase;
    private final ExchangeCoinQueryPort exchangeCoinQueryPort;
    private final LivePricePort livePricePort;

    @Override
    public EvaluatedHoldings findAllByWalletId(Long walletId, Long exchangeId) {
        List<HoldingInfoResult> activeHoldings = findActiveHoldingsUseCase.findActiveHoldings(walletId);

        if (activeHoldings.isEmpty()) {
            return new EvaluatedHoldings(List.of());
        }

        List<Long> coinIds = activeHoldings.stream().map(HoldingInfoResult::coinId).toList();
        Map<Long, Long> coinToExchangeCoinMap = exchangeCoinQueryPort.findExchangeCoinIdsByExchangeIdAndCoinIds(exchangeId, coinIds);
        Map<Long, BigDecimal> priceMap = livePricePort.getCurrentPrices(new ArrayList<>(coinToExchangeCoinMap.values()));

        List<EvaluatedHolding> holdings = activeHoldings.stream()
            .map(h -> toEvaluatedHolding(h, coinToExchangeCoinMap, priceMap))
            .toList();
        return new EvaluatedHoldings(holdings);
    }

    private EvaluatedHolding toEvaluatedHolding(HoldingInfoResult holding, Map<Long, Long> coinToExchangeCoinMap,
                                                 Map<Long, BigDecimal> priceMap) {
        Long exchangeCoinId = coinToExchangeCoinMap.get(holding.coinId());
        BigDecimal currentPrice = exchangeCoinId != null
            ? priceMap.getOrDefault(exchangeCoinId, BigDecimal.ZERO)
            : BigDecimal.ZERO;
        return EvaluatedHolding.create(
            holding.coinId(), holding.avgBuyPrice(),
            holding.totalQuantity(), currentPrice);
    }
}
