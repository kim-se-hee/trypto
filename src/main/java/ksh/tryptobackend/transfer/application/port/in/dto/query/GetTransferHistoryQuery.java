package ksh.tryptobackend.transfer.application.port.in.dto.query;

import ksh.tryptobackend.transfer.domain.vo.TransferType;

public record GetTransferHistoryQuery(
    Long walletId,
    TransferType type,
    Long cursorTransferId,
    int size
) {
}
