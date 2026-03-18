package ksh.tryptobackend.transfer.domain.vo;

import java.math.BigDecimal;

public sealed interface TransferBalanceChange {

    record Deduct(Long walletId, Long coinId, BigDecimal amount) implements TransferBalanceChange {}

    record Add(Long walletId, Long coinId, BigDecimal amount) implements TransferBalanceChange {}
}
