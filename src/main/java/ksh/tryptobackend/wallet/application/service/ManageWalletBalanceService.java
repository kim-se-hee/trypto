package ksh.tryptobackend.wallet.application.service;

import ksh.tryptobackend.wallet.application.port.in.ManageWalletBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceOperationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ManageWalletBalanceService implements ManageWalletBalanceUseCase {

    private final WalletBalanceOperationPort walletBalanceOperationPort;

    @Override
    @Transactional
    public void deductBalance(Long walletId, Long coinId, BigDecimal amount) {
        walletBalanceOperationPort.deductBalance(walletId, coinId, amount);
    }

    @Override
    @Transactional
    public void addBalance(Long walletId, Long coinId, BigDecimal amount) {
        walletBalanceOperationPort.addBalance(walletId, coinId, amount);
    }

    @Override
    @Transactional
    public void lockBalance(Long walletId, Long coinId, BigDecimal amount) {
        walletBalanceOperationPort.lockBalance(walletId, coinId, amount);
    }

    @Override
    @Transactional
    public void unlockBalance(Long walletId, Long coinId, BigDecimal amount) {
        walletBalanceOperationPort.unlockBalance(walletId, coinId, amount);
    }
}
