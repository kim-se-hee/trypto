package ksh.tryptobackend.wallet.domain.service;

import java.math.BigDecimal;

public interface HoldingMover {

    void move(Long fromWalletId, Long toWalletId, Long toExchangeId, Long coinId, BigDecimal amount);
}
