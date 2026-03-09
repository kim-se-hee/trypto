package ksh.tryptobackend.ranking.adapter.out;

import ksh.tryptobackend.marketdata.application.port.in.FindCoinSymbolsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeSummaryUseCase;
import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotDetailsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotDetailResult;
import ksh.tryptobackend.ranking.application.port.out.RankerHoldingQueryPort;
import ksh.tryptobackend.ranking.domain.vo.RankerHolding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RankerHoldingQueryAdapter implements RankerHoldingQueryPort {

    private final FindSnapshotDetailsUseCase findSnapshotDetailsUseCase;
    private final FindCoinSymbolsUseCase findCoinSymbolsUseCase;
    private final FindExchangeSummaryUseCase findExchangeSummaryUseCase;

    @Override
    public List<RankerHolding> findLatestHoldings(Long userId, Long roundId) {
        List<SnapshotDetailResult> details = findSnapshotDetailsUseCase.findLatestSnapshotDetails(userId, roundId);
        if (details.isEmpty()) {
            return List.of();
        }

        Map<Long, String> coinSymbols = resolveCoinSymbols(details);
        Map<Long, String> exchangeNames = resolveExchangeNames(details);

        return details.stream()
            .map(r -> new RankerHolding(
                coinSymbols.getOrDefault(r.coinId(), ""),
                exchangeNames.getOrDefault(r.exchangeId(), ""),
                r.assetRatio(), r.profitRate()))
            .toList();
    }

    private Map<Long, String> resolveCoinSymbols(List<SnapshotDetailResult> details) {
        Set<Long> coinIds = details.stream().map(SnapshotDetailResult::coinId).collect(Collectors.toSet());
        return findCoinSymbolsUseCase.findSymbolsByIds(coinIds);
    }

    private Map<Long, String> resolveExchangeNames(List<SnapshotDetailResult> details) {
        Set<Long> exchangeIds = details.stream().map(SnapshotDetailResult::exchangeId).collect(Collectors.toSet());
        return exchangeIds.stream()
            .collect(Collectors.toMap(
                id -> id,
                id -> findExchangeSummaryUseCase.findExchangeSummary(id)
                    .map(r -> r.name())
                    .orElse("")));
    }
}
