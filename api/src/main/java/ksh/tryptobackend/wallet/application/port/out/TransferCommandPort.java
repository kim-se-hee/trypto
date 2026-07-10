package ksh.tryptobackend.wallet.application.port.out;

import ksh.tryptobackend.wallet.domain.model.Transfer;

public interface TransferCommandPort {

    Transfer save(Transfer transfer);
}
