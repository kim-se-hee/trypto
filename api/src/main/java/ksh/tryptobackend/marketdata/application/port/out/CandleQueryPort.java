package ksh.tryptobackend.marketdata.application.port.out;

import java.util.List;
import ksh.tryptobackend.marketdata.domain.model.Candle;
import ksh.tryptobackend.marketdata.domain.model.CandleFilter;

public interface CandleQueryPort {

    List<Candle> findByFilter(CandleFilter filter);
}
