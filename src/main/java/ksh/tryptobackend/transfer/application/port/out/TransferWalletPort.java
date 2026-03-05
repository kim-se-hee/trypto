package ksh.tryptobackend.transfer.application.port.out;

import ksh.tryptobackend.transfer.application.port.out.dto.TransferWalletInfo;

import java.math.BigDecimal;

public interface TransferWalletPort {

    Long getOwnerUserId(Long walletId);

    TransferWalletInfo getWallet(Long walletId);

    BigDecimal getAvailableBalance(Long walletId, Long coinId);

    void deductBalance(Long walletId, Long coinId, BigDecimal amount);

    void addBalance(Long walletId, Long coinId, BigDecimal amount);

    void lockBalance(Long walletId, Long coinId, BigDecimal amount);
}
