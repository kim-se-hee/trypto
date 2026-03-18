package ksh.tryptobackend.marketdata.application.port.in;

import java.math.BigDecimal;

public interface BroadcastLiveTickerUseCase {

    void broadcast(String exchange, String symbol, BigDecimal currentPrice,
                   BigDecimal changeRate, BigDecimal quoteTurnover, Long timestamp);
}
