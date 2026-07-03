package ksh.tryptobackend.wallet.application.service;

import java.math.BigDecimal;
import ksh.tryptobackend.wallet.application.port.in.ManageWalletBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.out.WalletCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManageWalletBalanceService implements ManageWalletBalanceUseCase {

    private final WalletCommandPort walletCommandPort;

    @Override
    @Transactional
    public void deductBalance(Long walletId, Long coinId, BigDecimal amount) {
        walletCommandPort.deductBalance(walletId, coinId, amount);
    }

    @Override
    @Transactional
    public void addBalance(Long walletId, Long coinId, BigDecimal amount) {
        walletCommandPort.addBalance(walletId, coinId, amount);
    }

    @Override
    @Transactional
    public void lockBalance(Long walletId, Long coinId, BigDecimal amount) {
        walletCommandPort.lockBalance(walletId, coinId, amount);
    }

    @Override
    @Transactional
    public void unlockBalance(Long walletId, Long coinId, BigDecimal amount) {
        walletCommandPort.unlockBalance(walletId, coinId, amount);
    }

    @Override
    @Transactional
    public void consumeLocked(Long walletId, Long coinId, BigDecimal amount) {
        walletCommandPort.consumeLocked(walletId, coinId, amount);
    }
}
