package ksh.tryptobackend.transfer.application.port.in.dto.query;

import ksh.tryptobackend.transfer.domain.vo.TransferType;

public record FindTransferHistoryQuery(
    Long walletId,
    Long userId,
    TransferType type,
    Long cursorTransferId,
    int size
) {

    private static final int DEFAULT_SIZE = 20;

    public FindTransferHistoryQuery {
        if (type == null) {
            type = TransferType.ALL;
        }
        if (size == 0) {
            size = DEFAULT_SIZE;
        }
    }

    public FindTransferHistoryQuery(Long walletId, Long userId, TransferType type, Long cursorTransferId, Integer size) {
        this(walletId, userId, type, cursorTransferId, size == null ? DEFAULT_SIZE : size);
    }
}
