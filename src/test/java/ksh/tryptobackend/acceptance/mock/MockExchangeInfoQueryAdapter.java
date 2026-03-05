package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.ranking.application.port.out.ExchangeInfoQueryPort;
import ksh.tryptobackend.ranking.application.port.out.dto.ExchangeSnapshotInfo;
import ksh.tryptobackend.ranking.domain.vo.KrwConversionRate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockExchangeInfoQueryAdapter implements ExchangeInfoQueryPort {

    private final Map<Long, ExchangeSnapshotInfo> exchanges = new ConcurrentHashMap<>();

    @Override
    public ExchangeSnapshotInfo getExchangeInfo(Long exchangeId) {
        return exchanges.get(exchangeId);
    }

    public void addExchange(Long exchangeId, Long baseCurrencyCoinId, KrwConversionRate conversionRate) {
        exchanges.put(exchangeId, new ExchangeSnapshotInfo(exchangeId, baseCurrencyCoinId, conversionRate));
    }

    public void clear() {
        exchanges.clear();
    }
}
