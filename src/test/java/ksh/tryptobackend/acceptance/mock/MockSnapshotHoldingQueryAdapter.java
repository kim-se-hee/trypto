package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.ranking.application.port.out.EvaluatedHoldingQueryPort;
import ksh.tryptobackend.ranking.domain.model.EvaluatedHolding;
import ksh.tryptobackend.ranking.domain.model.EvaluatedHoldings;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockSnapshotHoldingQueryAdapter implements EvaluatedHoldingQueryPort {

    private final Map<String, List<EvaluatedHolding>> holdingsMap = new ConcurrentHashMap<>();

    @Override
    public EvaluatedHoldings findAllByWalletId(Long walletId, Long exchangeId) {
        List<EvaluatedHolding> holdings = holdingsMap.getOrDefault(key(walletId, exchangeId), List.of());
        return new EvaluatedHoldings(holdings);
    }

    public void addHolding(Long walletId, Long exchangeId, Long coinId,
                           BigDecimal avgBuyPrice, BigDecimal quantity, BigDecimal currentPrice) {
        holdingsMap.computeIfAbsent(key(walletId, exchangeId), k -> new ArrayList<>())
            .add(EvaluatedHolding.create(coinId, avgBuyPrice, quantity, currentPrice));
    }

    public void clear() {
        holdingsMap.clear();
    }

    private String key(Long walletId, Long exchangeId) {
        return walletId + ":" + exchangeId;
    }
}
