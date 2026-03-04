package ksh.tryptobackend.transfer.application.port.out;

import ksh.tryptobackend.transfer.domain.vo.WithdrawalCondition;

public interface TransferWithdrawalFeePort {

    WithdrawalCondition getWithdrawalFee(Long exchangeId, Long coinId, String chain);
}
