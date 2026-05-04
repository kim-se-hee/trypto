package ksh.tryptobackend.marketdata.application.port.in;

import java.math.BigDecimal;
import java.util.Optional;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.LiveTickerResult;

public interface ResolveLiveTickerUseCase {

    Optional<LiveTickerResult> resolve(
            String exchange,
            String symbol,
            BigDecimal currentPrice,
            BigDecimal changeRate,
            BigDecimal quoteTurnover,
            Long timestamp);
}
