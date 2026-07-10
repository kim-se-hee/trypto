package ksh.tryptobackend.wallet.domain.model;

import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

public class WalletBalances {

    private final List<WalletBalance> values;

    public WalletBalances(List<WalletBalance> values) {
        this.values = List.copyOf(values);
    }

    public WalletBalance getByCoinId(Long coinId) {
        return values.stream()
                .filter(balance -> balance.getCoinId().equals(coinId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.WALLET_BALANCE_NOT_FOUND));
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
