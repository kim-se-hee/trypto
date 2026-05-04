package ksh.tryptobackend.wallet.domain.model;

import java.util.List;

public class WalletBalances {

    private final List<WalletBalance> values;

    public WalletBalances(List<WalletBalance> values) {
        this.values = List.copyOf(values);
    }

    public WalletBalance getBaseCurrencyOrZero(Long baseCurrencyCoinId) {
        return values.stream()
                .filter(balance -> balance.getCoinId().equals(baseCurrencyCoinId))
                .findFirst()
                .orElse(WalletBalance.zero(baseCurrencyCoinId));
    }

    public List<WalletBalance> findCoinBalances(Long baseCurrencyCoinId) {
        return values.stream()
                .filter(balance -> !balance.getCoinId().equals(baseCurrencyCoinId))
                .toList();
    }
}
