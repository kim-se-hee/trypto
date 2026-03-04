package ksh.tryptobackend.transfer.domain.vo;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

public record TransferSourceExchange(Long baseCurrencyCoinId, boolean fiatCurrency) {

    public void validateTransferable(Long coinId) {
        if (fiatCurrency && baseCurrencyCoinId.equals(coinId)) {
            throw new CustomException(ErrorCode.BASE_CURRENCY_NOT_TRANSFERABLE);
        }
    }
}
