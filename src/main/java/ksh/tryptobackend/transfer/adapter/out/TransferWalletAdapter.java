package ksh.tryptobackend.transfer.adapter.out;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.transfer.application.port.out.TransferWalletPort;
import ksh.tryptobackend.transfer.application.port.out.dto.TransferWalletInfo;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceOperationPort;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class TransferWalletAdapter implements TransferWalletPort {

    private final WalletQueryPort walletQueryPort;
    private final WalletBalanceOperationPort walletBalanceOperationPort;

    @Override
    public TransferWalletInfo getWallet(Long walletId) {
        return walletQueryPort.findById(walletId)
            .map(info -> new TransferWalletInfo(info.walletId(), info.roundId(), info.exchangeId()))
            .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
    }

    @Override
    public BigDecimal getAvailableBalance(Long walletId, Long coinId) {
        return walletBalanceOperationPort.getAvailableBalance(walletId, coinId);
    }

    @Override
    public void deductBalance(Long walletId, Long coinId, BigDecimal amount) {
        walletBalanceOperationPort.deductBalance(walletId, coinId, amount);
    }

    @Override
    public void addBalance(Long walletId, Long coinId, BigDecimal amount) {
        walletBalanceOperationPort.addBalance(walletId, coinId, amount);
    }

    @Override
    public void lockBalance(Long walletId, Long coinId, BigDecimal amount) {
        walletBalanceOperationPort.lockBalance(walletId, coinId, amount);
    }
}
