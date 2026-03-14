package ksh.tryptobackend.marketdata.adapter.in.dto.response;

import ksh.tryptobackend.marketdata.application.port.in.dto.result.WithdrawalFeeResult;

import java.math.BigDecimal;

public record WithdrawalFeeResponse(
    BigDecimal fee,
    BigDecimal minWithdrawal
) {

    public static WithdrawalFeeResponse from(WithdrawalFeeResult result) {
        return new WithdrawalFeeResponse(result.fee(), result.minWithdrawal());
    }
}
