package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.model.Holding;

import java.util.List;

public interface HoldingQueryPort {

    List<Holding> findAllByWalletId(Long walletId);
}
