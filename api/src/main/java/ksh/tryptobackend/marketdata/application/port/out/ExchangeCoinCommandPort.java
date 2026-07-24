package ksh.tryptobackend.marketdata.application.port.out;

import java.util.Optional;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoin;

public interface ExchangeCoinCommandPort {

    ExchangeCoin save(Long exchangeId, Long coinId, String displayName);

    ExchangeCoin register(Long exchangeId, Long coinId, String displayName);

    Optional<ExchangeCoin> suspend(Long exchangeId, Long coinId);
}
