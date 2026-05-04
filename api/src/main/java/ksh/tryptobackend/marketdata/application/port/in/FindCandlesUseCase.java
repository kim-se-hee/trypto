package ksh.tryptobackend.marketdata.application.port.in;

import java.util.List;
import ksh.tryptobackend.marketdata.application.port.in.dto.query.FindCandlesQuery;
import ksh.tryptobackend.marketdata.domain.model.Candle;

public interface FindCandlesUseCase {

    List<Candle> findCandles(FindCandlesQuery query);
}
