package ksh.tryptobackend.trading.application.port.out;

import java.util.Optional;

public interface ExchangeCoinPort {

    Optional<ExchangeCoinData> findById(Long exchangeCoinId);

    record ExchangeCoinData(
        Long exchangeCoinId,
        Long exchangeId,
        Long coinId
    ) {
    }
}
