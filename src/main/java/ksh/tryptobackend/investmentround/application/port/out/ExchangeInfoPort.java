package ksh.tryptobackend.investmentround.application.port.out;

import ksh.tryptobackend.investmentround.application.port.out.dto.ExchangeInfo;

import java.util.Optional;

public interface ExchangeInfoPort {

    Optional<ExchangeInfo> findById(Long exchangeId);
}
