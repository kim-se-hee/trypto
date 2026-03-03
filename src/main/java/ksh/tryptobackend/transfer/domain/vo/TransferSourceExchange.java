package ksh.tryptobackend.transfer.domain.vo;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

import java.util.Objects;

public class TransferSourceExchange {

    private final Long baseCurrencyCoinId;
    private final boolean fiatCurrency;

    private TransferSourceExchange(Long baseCurrencyCoinId, boolean fiatCurrency) {
        this.baseCurrencyCoinId = baseCurrencyCoinId;
        this.fiatCurrency = fiatCurrency;
    }

    public static TransferSourceExchange of(Long baseCurrencyCoinId, boolean fiatCurrency) {
        return new TransferSourceExchange(baseCurrencyCoinId, fiatCurrency);
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
        TransferSourceExchange that = (TransferSourceExchange) o;
        return fiatCurrency == that.fiatCurrency
            && Objects.equals(baseCurrencyCoinId, that.baseCurrencyCoinId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseCurrencyCoinId, fiatCurrency);
    }
}
