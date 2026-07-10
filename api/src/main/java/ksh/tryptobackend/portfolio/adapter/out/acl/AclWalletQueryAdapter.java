package ksh.tryptobackend.portfolio.adapter.out.acl;

import java.math.BigDecimal;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.portfolio.application.port.out.WalletQueryPort;
import ksh.tryptobackend.portfolio.domain.vo.PortfolioWallet;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.GetAvailableBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.in.GetWalletOwnerIdUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("portfolioAclWalletQueryAdapter")
@RequiredArgsConstructor
public class AclWalletQueryAdapter implements WalletQueryPort {

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
}
