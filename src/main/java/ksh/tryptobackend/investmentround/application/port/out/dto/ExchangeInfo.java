package ksh.tryptobackend.investmentround.application.port.out.dto;

import ksh.tryptobackend.investmentround.domain.vo.SeedAmountPolicy;

public record ExchangeInfo(Long baseCurrencyCoinId, SeedAmountPolicy seedAmountPolicy) {
}
