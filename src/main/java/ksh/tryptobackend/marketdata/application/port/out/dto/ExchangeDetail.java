package ksh.tryptobackend.marketdata.application.port.out.dto;

public record ExchangeDetail(String name, Long baseCurrencyCoinId, boolean domestic) {
}
