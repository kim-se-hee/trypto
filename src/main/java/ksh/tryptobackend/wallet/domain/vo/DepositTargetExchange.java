package ksh.tryptobackend.wallet.domain.vo;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import lombok.Getter;

import java.util.Objects;

@Getter
public class DepositTargetExchange {

    private final Long baseCurrencyCoinId;
    private final boolean fiatCurrency;

    private DepositTargetExchange(Long baseCurrencyCoinId, boolean fiatCurrency) {
        this.baseCurrencyCoinId = baseCurrencyCoinId;
        this.fiatCurrency = fiatCurrency;
    }

    public static DepositTargetExchange of(Long baseCurrencyCoinId, boolean fiatCurrency) {
        return new DepositTargetExchange(baseCurrencyCoinId, fiatCurrency);
    }

    public void validateTransferable(Long coinId) {
        if (fiatCurrency && baseCurrencyCoinId.equals(coinId)) {
            throw new CustomException(ErrorCode.BASE_CURRENCY_NOT_TRANSFERABLE);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepositTargetExchange that = (DepositTargetExchange) o;
        return fiatCurrency == that.fiatCurrency
            && Objects.equals(baseCurrencyCoinId, that.baseCurrencyCoinId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseCurrencyCoinId, fiatCurrency);
    }
}
