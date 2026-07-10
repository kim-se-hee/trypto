package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.domain.model.ViolatedOrders;

public interface TradingQueryPort {

    ViolatedOrders findViolatedOrders(Long roundId, Long exchangeId, Long walletId);
}
