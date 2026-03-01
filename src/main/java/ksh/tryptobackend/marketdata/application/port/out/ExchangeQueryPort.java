package ksh.tryptobackend.marketdata.application.port.out;

import ksh.tryptobackend.marketdata.application.port.out.dto.ExchangeDetail;

import java.util.Optional;

public interface ExchangeQueryPort {

    Optional<ExchangeDetail> findExchangeDetailById(Long exchangeId);
}
