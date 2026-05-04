package ksh.tryptobackend.marketdata.application.port.in.dto.result;

import java.math.BigDecimal;

public record WithdrawalFeeResult(BigDecimal fee, BigDecimal minWithdrawal) {}
