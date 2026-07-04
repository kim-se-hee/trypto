package ksh.tryptobackend.investmentround.domain.service;

import java.math.BigDecimal;

public interface WalletBalanceService {

    void addAvailable(Long walletId, Long coinId, BigDecimal amount);
}
