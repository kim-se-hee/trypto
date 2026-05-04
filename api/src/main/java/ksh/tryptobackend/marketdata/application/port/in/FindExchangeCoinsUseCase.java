package ksh.tryptobackend.marketdata.application.port.in;

import java.util.List;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinListResult;

public interface FindExchangeCoinsUseCase {

    List<ExchangeCoinListResult> findByExchangeId(Long exchangeId);
}
