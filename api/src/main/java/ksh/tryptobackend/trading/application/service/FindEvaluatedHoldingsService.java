package ksh.tryptobackend.trading.application.service;

import java.math.BigDecimal;
import java.util.List;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.GetLivePriceUseCase;
import ksh.tryptobackend.trading.application.port.in.FindActiveHoldingsUseCase;
import ksh.tryptobackend.trading.application.port.in.FindEvaluatedHoldingsUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.EvaluatedHoldingResult;
import ksh.tryptobackend.trading.application.port.in.dto.result.HoldingInfoResult;
import ksh.tryptobackend.trading.domain.vo.CoinExchangeMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

        CoinExchangeMapping coinExchangeMapping = findCoinExchangeMapping(exchangeId, holdings);

        return holdings.stream()
                .map(holding -> toEvaluatedHoldingResult(holding, coinExchangeMapping))
                .toList();
    }

    private CoinExchangeMapping findCoinExchangeMapping(
            Long exchangeId, List<HoldingInfoResult> holdings) {
        List<Long> coinIds = holdings.stream().map(HoldingInfoResult::coinId).toList();
        return new CoinExchangeMapping(
                findExchangeCoinMappingUseCase.findExchangeCoinIdMap(exchangeId, coinIds));
    }

    private EvaluatedHoldingResult toEvaluatedHoldingResult(
            HoldingInfoResult holding, CoinExchangeMapping coinExchangeMapping) {
        Long exchangeCoinId = coinExchangeMapping.getExchangeCoinId(holding.coinId());
        BigDecimal currentPrice = getLivePriceUseCase.getCurrentPrice(exchangeCoinId);
        return new EvaluatedHoldingResult(
                holding.coinId(), holding.avgBuyPrice(), holding.totalQuantity(), currentPrice);
    }
}
