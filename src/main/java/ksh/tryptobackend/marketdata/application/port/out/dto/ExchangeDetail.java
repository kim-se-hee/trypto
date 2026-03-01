package ksh.tryptobackend.marketdata.application.port.out.dto;

import ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType;

public record ExchangeDetail(Long baseCurrencyCoinId, ExchangeMarketType marketType) {
}
