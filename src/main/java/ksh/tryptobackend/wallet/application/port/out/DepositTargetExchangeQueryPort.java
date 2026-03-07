package ksh.tryptobackend.wallet.application.port.out;

import ksh.tryptobackend.wallet.domain.vo.DepositTargetExchange;

public interface DepositTargetExchangeQueryPort {

    Long getExchangeIdByWalletId(Long walletId);

    DepositTargetExchange getExchange(Long exchangeId);

    boolean isTagRequired(Long exchangeId, Long coinId, String chain);
}
