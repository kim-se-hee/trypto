package ksh.tryptobackend.marketdata.adapter.in.dto.response;

import java.math.BigDecimal;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.WithdrawalFeeResult;

public record WithdrawalFeeResponse(BigDecimal fee, BigDecimal minWithdrawal) {

    public static WithdrawalFeeResponse from(WithdrawalFeeResult result) {
        return new WithdrawalFeeResponse(result.fee(), result.minWithdrawal());
    }
}
