package ksh.tryptobackend.marketdata.application.port.in;

import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinListResult;

import java.util.List;

public interface FindExchangeCoinsUseCase {

    List<ExchangeCoinListResult> findByExchangeId(Long exchangeId);
}
