package ksh.tryptobackend.transfer.application.port.out;

import java.util.List;
import ksh.tryptobackend.transfer.domain.model.Transfer;
import ksh.tryptobackend.transfer.domain.vo.TransferType;

public interface TransferQueryPort {

    List<Transfer> findByCursor(Long walletId, TransferType type, Long cursorTransferId, int size);
}
