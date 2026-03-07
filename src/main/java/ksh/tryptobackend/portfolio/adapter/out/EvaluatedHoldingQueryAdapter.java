package ksh.tryptobackend.portfolio.adapter.out;

import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.portfolio.application.port.out.EvaluatedHoldingQueryPort;
import ksh.tryptobackend.portfolio.domain.model.EvaluatedHolding;
import ksh.tryptobackend.portfolio.domain.model.EvaluatedHoldings;
import ksh.tryptobackend.trading.application.port.in.FindActiveHoldingsUseCase;
import ksh.tryptobackend.trading.application.port.in.GetLivePriceUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.HoldingInfoResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EvaluatedHoldingQueryAdapter implements EvaluatedHoldingQueryPort {

    private final FindActiveHoldingsUseCase findActiveHoldingsUseCase;
    private final FindExchangeCoinMappingUseCase findExchangeCoinMappingUseCase;
    private final GetLivePriceUseCase getLivePriceUseCase;

    @Override
    public EvaluatedHoldings findAllByWalletId(Long walletId, Long exchangeId) {
        List<HoldingInfoResult> activeHoldings = findActiveHoldingsUseCase.findActiveHoldings(walletId);

        if (activeHoldings.isEmpty()) {
            return new EvaluatedHoldings(List.of());
        }

        List<Long> coinIds = activeHoldings.stream().map(HoldingInfoResult::coinId).toList();
        Map<Long, Long> coinToExchangeCoinMap = findExchangeCoinMappingUseCase.findExchangeCoinIdMap(exchangeId, coinIds);
        Map<Long, BigDecimal> priceMap = resolvePrices(new ArrayList<>(coinToExchangeCoinMap.values()));

        List<EvaluatedHolding> holdings = activeHoldings.stream()
            .map(h -> toEvaluatedHolding(h, coinToExchangeCoinMap, priceMap))
            .toList();
        return new EvaluatedHoldings(holdings);
    }

    private Map<Long, BigDecimal> resolvePrices(List<Long> exchangeCoinIds) {
        return exchangeCoinIds.stream()
            .collect(Collectors.toMap(id -> id, getLivePriceUseCase::getCurrentPrice));
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
