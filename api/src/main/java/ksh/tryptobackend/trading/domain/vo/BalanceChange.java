package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;

public sealed interface BalanceChange {

    record Lock(Long coinId, BigDecimal amount) implements BalanceChange {}

    record Unlock(Long coinId, BigDecimal amount) implements BalanceChange {}

    record AddAvailable(Long coinId, BigDecimal amount) implements BalanceChange {}

    record ConsumeLocked(Long coinId, BigDecimal amount) implements BalanceChange {}
}
