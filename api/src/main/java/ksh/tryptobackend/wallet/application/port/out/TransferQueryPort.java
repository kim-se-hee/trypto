package ksh.tryptobackend.wallet.application.port.out;

import java.util.List;
import ksh.tryptobackend.wallet.domain.model.Transfer;
import ksh.tryptobackend.wallet.domain.vo.TransferType;

public interface TransferQueryPort {

    List<Transfer> findByCursor(Long walletId, TransferType type, Long cursorTransferId, int size);
}
