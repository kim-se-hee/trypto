package ksh.tryptobackend.investmentround.domain.service;

import java.math.BigDecimal;

public interface FundsDepositor {

    void deposit(Long walletId, Long coinId, BigDecimal amount);
}
