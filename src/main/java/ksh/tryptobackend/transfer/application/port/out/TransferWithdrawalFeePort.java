package ksh.tryptobackend.transfer.application.port.out;

import ksh.tryptobackend.transfer.application.port.out.dto.TransferWithdrawalFeeInfo;

public interface TransferWithdrawalFeePort {

    TransferWithdrawalFeeInfo getWithdrawalFee(Long exchangeId, Long coinId, String chain);
}
