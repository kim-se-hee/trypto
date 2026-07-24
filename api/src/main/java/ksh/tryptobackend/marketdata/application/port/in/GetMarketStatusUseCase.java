package ksh.tryptobackend.marketdata.application.port.in;

public interface GetMarketStatusUseCase {

    boolean isSuspended(Long exchangeCoinId);
}
