package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.marketdata.application.port.out.CandleQueryPort;
import ksh.tryptobackend.marketdata.domain.model.Candle;
import ksh.tryptobackend.marketdata.domain.model.CandleFilter;

import java.util.ArrayList;
import java.util.List;

public class MockCandleAdapter implements CandleQueryPort {

    private final List<Candle> candles = new ArrayList<>();

    @Override
    public List<Candle> findByFilter(CandleFilter filter) {
        return List.copyOf(candles);
    }

    public void setCandles(List<Candle> candles) {
        this.candles.clear();
        this.candles.addAll(candles);
    }

    public void clear() {
        candles.clear();
    }
}
