package ksh.tryptobackend.marketdata.application.port.out;

import ksh.tryptobackend.marketdata.application.port.out.dto.ExchangeDetail;
import ksh.tryptobackend.marketdata.application.port.out.dto.ExchangeSummary;

import java.util.Optional;

public interface ExchangeQueryPort {

    Optional<ExchangeDetail> findExchangeDetailById(Long exchangeId);

    Optional<ExchangeSummary> findExchangeSummaryById(Long exchangeId);
}
