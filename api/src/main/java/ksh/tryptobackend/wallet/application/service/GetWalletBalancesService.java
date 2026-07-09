package ksh.tryptobackend.wallet.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.wallet.application.port.in.GetWalletBalancesUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.query.GetWalletBalancesQuery;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletBalancesResult;
import ksh.tryptobackend.wallet.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.wallet.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceQueryPort;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import ksh.tryptobackend.wallet.domain.model.Wallet;
import ksh.tryptobackend.wallet.domain.model.WalletBalances;
import ksh.tryptobackend.wallet.domain.vo.BaseCurrency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetWalletBalancesService implements GetWalletBalancesUseCase {

    private final WalletQueryPort walletQueryPort;
    private final WalletBalanceQueryPort walletBalanceQueryPort;
    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final MarketDataQueryPort marketDataQueryPort;

    @Override
    @Transactional(readOnly = true)
    public WalletBalancesResult getWalletBalances(GetWalletBalancesQuery query) {
        Wallet wallet =
                walletQueryPort
                        .findById(query.walletId())
                        .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
        wallet.verifyOwnedBy(
                query.userId(), investmentRoundQueryPort.getOwnerId(wallet.getRoundId()));

        BaseCurrency baseCurrency = marketDataQueryPort.getBaseCurrency(wallet.getExchangeId());
        WalletBalances balances =
                new WalletBalances(walletBalanceQueryPort.findByWalletId(query.walletId()));

        return WalletBalancesResult.of(wallet.getExchangeId(), baseCurrency, balances);
    }
}
