package ksh.tryptobackend.marketdata.application.port.out;

import ksh.tryptobackend.marketdata.domain.model.Candle;
import ksh.tryptobackend.marketdata.domain.model.CandleFilter;

import java.util.List;

public interface CandleQueryPort {

    List<Candle> findByFilter(CandleFilter filter);
}
