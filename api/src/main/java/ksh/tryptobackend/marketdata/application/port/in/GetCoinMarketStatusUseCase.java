package ksh.tryptobackend.marketdata.application.port.in;

public interface GetCoinMarketStatusUseCase {

    boolean isSuspended(Long exchangeId, Long coinId);
}
