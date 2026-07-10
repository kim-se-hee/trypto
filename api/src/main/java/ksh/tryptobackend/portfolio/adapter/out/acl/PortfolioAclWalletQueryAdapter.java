package ksh.tryptobackend.portfolio.adapter.out.acl;

import java.math.BigDecimal;
import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.portfolio.application.port.out.WalletQueryPort;
import ksh.tryptobackend.portfolio.domain.vo.PortfolioWallet;
import ksh.tryptobackend.portfolio.domain.vo.WalletSnapshot;
import ksh.tryptobackend.portfolio.domain.vo.WalletSnapshots;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.GetAvailableBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.in.GetWalletOwnerIdUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortfolioAclWalletQueryAdapter implements WalletQueryPort {

    private final FindWalletUseCase findWalletUseCase;
    private final GetWalletOwnerIdUseCase getWalletOwnerIdUseCase;
    private final GetAvailableBalanceUseCase getAvailableBalanceUseCase;

    @Override
    public PortfolioWallet getWallet(Long walletId) {
        WalletResult wallet =
                findWalletUseCase
                        .findById(walletId)
                        .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
        Long ownerId = getWalletOwnerIdUseCase.getWalletOwnerId(walletId);
        return new PortfolioWallet(wallet.walletId(), wallet.exchangeId(), ownerId);
    }

    @Override
    public BigDecimal getBaseCurrencyBalance(Long walletId, Long baseCurrencyCoinId) {
        return getAvailableBalanceUseCase.getTotalBalance(walletId, baseCurrencyCoinId);
    }

    @Override
    public BigDecimal getAvailableBalance(Long walletId, Long coinId) {
        return getAvailableBalanceUseCase.getAvailableBalance(walletId, coinId);
    }

    @Override
    public WalletSnapshots findWalletSnapshots(List<Long> roundIds) {
        List<WalletSnapshot> wallets =
                findWalletUseCase.findByRoundIds(roundIds).stream()
                        .map(this::toWalletSnapshot)
                        .toList();
        return new WalletSnapshots(wallets);
    }

    private WalletSnapshot toWalletSnapshot(WalletResult result) {
        return new WalletSnapshot(
                result.walletId(), result.roundId(), result.exchangeId(), result.seedAmount());
    }
}
