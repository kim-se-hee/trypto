package ksh.tryptobackend.transfer.application.port.out.dto;

import java.math.BigDecimal;

public record TransferWithdrawalFeeInfo(BigDecimal fee, BigDecimal minWithdrawal) {
}
