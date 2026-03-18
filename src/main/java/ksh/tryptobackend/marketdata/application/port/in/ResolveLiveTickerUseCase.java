package ksh.tryptobackend.marketdata.application.port.in;

import ksh.tryptobackend.marketdata.application.port.in.dto.result.LiveTickerResult;

import java.math.BigDecimal;
import java.util.Optional;

public interface ResolveLiveTickerUseCase {

    Optional<LiveTickerResult> resolve(String exchange, String symbol, BigDecimal currentPrice,
                                       BigDecimal changeRate, BigDecimal quoteTurnover, Long timestamp);
}
