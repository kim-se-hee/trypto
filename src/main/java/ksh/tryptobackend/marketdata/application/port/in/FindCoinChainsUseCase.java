package ksh.tryptobackend.marketdata.application.port.in;

import ksh.tryptobackend.marketdata.domain.model.ExchangeCoinChain;

import java.util.List;

public interface FindCoinChainsUseCase {

    List<ExchangeCoinChain> findByExchangeIdAndCoinId(Long exchangeId, Long coinId);
}
