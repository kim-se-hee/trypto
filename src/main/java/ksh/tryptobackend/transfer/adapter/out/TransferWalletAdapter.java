package ksh.tryptobackend.transfer.adapter.out;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.FindRoundInfoUseCase;
import ksh.tryptobackend.transfer.application.port.out.TransferWalletPort;
import ksh.tryptobackend.transfer.domain.vo.TransferWallet;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.GetAvailableBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.in.ManageWalletBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class TransferWalletAdapter implements TransferWalletPort {

    private final FindWalletUseCase findWalletUseCase;
    private final FindRoundInfoUseCase findRoundInfoUseCase;
    private final GetAvailableBalanceUseCase getAvailableBalanceUseCase;
    private final ManageWalletBalanceUseCase manageWalletBalanceUseCase;

    @Override
    public Long getOwnerUserId(Long walletId) {
        WalletResult wallet = findWalletUseCase.findById(walletId)
            .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
        return findRoundInfoUseCase.findById(wallet.roundId())
            .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_FOUND))
            .userId();
    }

    @Override
    public TransferWallet getWallet(Long walletId) {
        return findWalletUseCase.findById(walletId)
            .map(result -> new TransferWallet(result.walletId(), result.roundId(), result.exchangeId()))
            .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
    }

    @Override
    public BigDecimal getAvailableBalance(Long walletId, Long coinId) {
        return getAvailableBalanceUseCase.getAvailableBalance(walletId, coinId);
    }

    @Override
    public void deductBalance(Long walletId, Long coinId, BigDecimal amount) {
        manageWalletBalanceUseCase.deductBalance(walletId, coinId, amount);
    }

    @Override
    public void addBalance(Long walletId, Long coinId, BigDecimal amount) {
        manageWalletBalanceUseCase.addBalance(walletId, coinId, amount);
    }

    @Override
    public void lockBalance(Long walletId, Long coinId, BigDecimal amount) {
        manageWalletBalanceUseCase.lockBalance(walletId, coinId, amount);
    }
}
