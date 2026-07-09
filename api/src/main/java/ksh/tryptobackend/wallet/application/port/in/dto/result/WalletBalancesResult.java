package ksh.tryptobackend.wallet.application.port.in.dto.result;

import java.math.BigDecimal;
import java.util.List;
import ksh.tryptobackend.wallet.domain.model.WalletBalance;
import ksh.tryptobackend.wallet.domain.model.WalletBalances;
import ksh.tryptobackend.wallet.domain.vo.BaseCurrency;

public record WalletBalancesResult(
        Long exchangeId,
        String baseCurrencySymbol,
        BigDecimal baseCurrencyAvailable,
        BigDecimal baseCurrencyLocked,
        List<CoinBalance> balances) {

    public record CoinBalance(Long coinId, BigDecimal available, BigDecimal locked) {}

    public static WalletBalancesResult of(
            Long exchangeId, BaseCurrency baseCurrency, WalletBalances balances) {
        WalletBalance baseBalance = balances.getBaseCurrencyOrZero(baseCurrency.coinId());
        List<CoinBalance> coinBalances =
                balances.findCoinBalances(baseCurrency.coinId()).stream()
                        .map(b -> new CoinBalance(b.getCoinId(), b.getAvailable(), b.getLocked()))
                        .toList();

        return new WalletBalancesResult(
                exchangeId,
                baseCurrency.symbol(),
                baseBalance.getAvailable(),
                baseBalance.getLocked(),
                coinBalances);
    }
}
