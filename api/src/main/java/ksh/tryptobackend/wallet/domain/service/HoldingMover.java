package ksh.tryptobackend.wallet.domain.service;

import ksh.tryptobackend.wallet.domain.model.Transfer;

public interface HoldingMover {

    void move(Transfer transfer, Long toExchangeId);
}
