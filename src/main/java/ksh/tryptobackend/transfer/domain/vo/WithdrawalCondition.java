package ksh.tryptobackend.transfer.domain.vo;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

import java.math.BigDecimal;

public record WithdrawalCondition(BigDecimal fee, BigDecimal minWithdrawal) {

    public void validateMinWithdrawal(BigDecimal amount) {
        if (amount.compareTo(minWithdrawal) < 0) {
            throw new CustomException(ErrorCode.BELOW_MIN_WITHDRAWAL);
        }
    }

    public void validateSufficientBalance(BigDecimal available, BigDecimal amount) {
        BigDecimal required = amount.add(fee);
        if (available.compareTo(required) < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }
}
