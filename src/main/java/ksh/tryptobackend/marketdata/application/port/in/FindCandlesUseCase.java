package ksh.tryptobackend.marketdata.application.port.in;

import ksh.tryptobackend.marketdata.application.port.in.dto.query.FindCandlesQuery;
import ksh.tryptobackend.marketdata.domain.model.Candle;

import java.util.List;

public interface FindCandlesUseCase {

    List<Candle> findCandles(FindCandlesQuery query);
}
