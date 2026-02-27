package ksh.tryptobackend.marketdata.application.port.out;

import ksh.tryptobackend.marketdata.domain.model.Exchange;

import java.util.Optional;

public interface ExchangePort {

    Optional<Exchange> findById(Long exchangeId);
}
