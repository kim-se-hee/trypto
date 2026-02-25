package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;

public sealed interface BalanceChange {

    record Deduct(Long coinId, BigDecimal amount) implements BalanceChange {}

    record Add(Long coinId, BigDecimal amount) implements BalanceChange {}

    record Lock(Long coinId, BigDecimal amount) implements BalanceChange {}
}
