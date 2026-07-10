package ksh.tryptobackend.trading.adapter.out.acl;

import java.math.BigDecimal;
import java.util.List;
import ksh.tryptobackend.trading.application.port.out.WalletQueryPort;
import ksh.tryptobackend.trading.domain.vo.WalletRef;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.GetAvailableBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.in.GetWalletOwnerIdUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("tradingAclWalletQueryAdapter")
@RequiredArgsConstructor
public class AclWalletQueryAdapter implements WalletQueryPort {

    private final GetWalletOwnerIdUseCase getWalletOwnerIdUseCase;
    private final GetAvailableBalanceUseCase getAvailableBalanceUseCase;
    private final FindWalletUseCase findWalletUseCase;

    @Override
    public Long getOwnerId(Long walletId) {
        return getWalletOwnerIdUseCase.getWalletOwnerId(walletId);
    }

    @Override
    public BigDecimal getAvailableBalance(Long walletId, Long coinId) {
        return getAvailableBalanceUseCase.getAvailableBalance(walletId, coinId);
    }

    @Override
    public List<WalletRef> findByRoundIds(List<Long> roundIds) {
        return findWalletUseCase.findByRoundIds(roundIds).stream().map(this::toWalletRef).toList();
    }

    @Override
    public List<Long> findWalletIdsByExchangeId(Long exchangeId) {
        return findWalletUseCase.findByExchangeId(exchangeId).stream()
                .map(WalletResult::walletId)
                .toList();
    }

    private WalletRef toWalletRef(WalletResult wallet) {
        return new WalletRef(wallet.walletId(), wallet.roundId());
    }
}
