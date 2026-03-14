package ksh.tryptobackend.wallet.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.FindRoundInfoUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RoundInfoResult;
import ksh.tryptobackend.marketdata.application.port.in.FindCoinInfoUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.CoinInfoResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.wallet.application.port.in.GetWalletBalancesUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.query.GetWalletBalancesQuery;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletBalancesResult;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletBalancesResult.BalanceDetail;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceQueryPort;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import ksh.tryptobackend.wallet.domain.model.Wallet;
import ksh.tryptobackend.wallet.domain.model.WalletBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GetWalletBalancesService implements GetWalletBalancesUseCase {

    private final WalletQueryPort walletQueryPort;
    private final WalletBalanceQueryPort walletBalanceQueryPort;

    private final FindRoundInfoUseCase findRoundInfoUseCase;
    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final FindCoinInfoUseCase findCoinInfoUseCase;

    @Override
    @Transactional(readOnly = true)
    public WalletBalancesResult getWalletBalances(GetWalletBalancesQuery query) {
        Wallet wallet = getWallet(query);
        validateOwnership(wallet, query);

        Long baseCurrencyCoinId = getBaseCurrencyCoinId(wallet);
        List<WalletBalance> balances = walletBalanceQueryPort.findByWalletId(query.walletId());
        String baseCurrencySymbol = getBaseCurrencySymbol(baseCurrencyCoinId);

        return buildResult(wallet, baseCurrencySymbol, baseCurrencyCoinId, balances);
    }

    private Wallet getWallet(GetWalletBalancesQuery query) {
        return walletQueryPort.findById(query.walletId())
            .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
    }

    private void validateOwnership(Wallet wallet, GetWalletBalancesQuery query) {
        RoundInfoResult round = findRoundInfoUseCase.findById(wallet.getRoundId())
            .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_FOUND));

        if (!round.userId().equals(query.userId())) {
            throw new CustomException(ErrorCode.WALLET_NOT_OWNED);
        }
    }

    private Long getBaseCurrencyCoinId(Wallet wallet) {
        ExchangeDetailResult exchange = findExchangeDetailUseCase.findExchangeDetail(wallet.getExchangeId())
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));

        return exchange.baseCurrencyCoinId();
    }

    private String getBaseCurrencySymbol(Long baseCurrencyCoinId) {
        CoinInfoResult coinInfo = findCoinInfoUseCase.findByIds(Set.of(baseCurrencyCoinId))
            .get(baseCurrencyCoinId);

        if (coinInfo == null) {
            throw new CustomException(ErrorCode.COIN_NOT_FOUND);
        }

        return coinInfo.symbol();
    }

    private WalletBalancesResult buildResult(Wallet wallet, String baseCurrencySymbol,
                                              Long baseCurrencyCoinId, List<WalletBalance> balances) {
        BigDecimal baseCurrencyAvailable = BigDecimal.ZERO;
        BigDecimal baseCurrencyLocked = BigDecimal.ZERO;
        List<BalanceDetail> coinBalances = new java.util.ArrayList<>();

        for (WalletBalance balance : balances) {
            if (balance.getCoinId().equals(baseCurrencyCoinId)) {
                baseCurrencyAvailable = balance.getAvailable();
                baseCurrencyLocked = balance.getLocked();
            } else {
                coinBalances.add(new BalanceDetail(
                    balance.getCoinId(),
                    balance.getAvailable(),
                    balance.getLocked()
                ));
            }
        }

        return new WalletBalancesResult(
            wallet.getExchangeId(),
            baseCurrencySymbol,
            baseCurrencyAvailable,
            baseCurrencyLocked,
            coinBalances
        );
    }
}
