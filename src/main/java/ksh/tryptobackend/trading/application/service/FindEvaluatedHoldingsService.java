package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.GetLivePriceUseCase;
import ksh.tryptobackend.trading.application.port.in.FindActiveHoldingsUseCase;
import ksh.tryptobackend.trading.application.port.in.FindEvaluatedHoldingsUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.EvaluatedHoldingResult;
import ksh.tryptobackend.trading.application.port.in.dto.result.HoldingInfoResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FindEvaluatedHoldingsService implements FindEvaluatedHoldingsUseCase {

    private final FindActiveHoldingsUseCase findActiveHoldingsUseCase;
    private final FindExchangeCoinMappingUseCase findExchangeCoinMappingUseCase;
    private final GetLivePriceUseCase getLivePriceUseCase;

    @Override
    public List<EvaluatedHoldingResult> findEvaluatedHoldings(Long walletId, Long exchangeId) {
        List<HoldingInfoResult> holdings = findActiveHoldingsUseCase.findActiveHoldings(walletId);
        if (holdings.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> exchangeCoinIdMap = findExchangeCoinIdMap(exchangeId, holdings);

        return holdings.stream()
            .map(holding -> toEvaluatedHoldingResult(holding, exchangeCoinIdMap))
            .toList();
    }

    private Map<Long, Long> findExchangeCoinIdMap(Long exchangeId, List<HoldingInfoResult> holdings) {
        List<Long> coinIds = holdings.stream().map(HoldingInfoResult::coinId).toList();
        return findExchangeCoinMappingUseCase.findExchangeCoinIdMap(exchangeId, coinIds);
    }

    private EvaluatedHoldingResult toEvaluatedHoldingResult(HoldingInfoResult holding,
                                                             Map<Long, Long> exchangeCoinIdMap) {
        Long exchangeCoinId = exchangeCoinIdMap.get(holding.coinId());
        BigDecimal currentPrice = getLivePriceUseCase.getCurrentPrice(exchangeCoinId);
        return new EvaluatedHoldingResult(
            holding.coinId(), holding.avgBuyPrice(), holding.totalQuantity(), currentPrice);
    }
}
