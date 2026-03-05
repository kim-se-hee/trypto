package ksh.tryptobackend.ranking.application.port.out.dto;

import ksh.tryptobackend.ranking.domain.vo.KrwConversionRate;

public record ExchangeSnapshotInfo(Long exchangeId, Long baseCurrencyCoinId, KrwConversionRate conversionRate) {
}
