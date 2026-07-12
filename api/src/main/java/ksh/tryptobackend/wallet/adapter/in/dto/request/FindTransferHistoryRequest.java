package ksh.tryptobackend.wallet.adapter.in.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import ksh.tryptobackend.wallet.application.port.in.dto.query.FindTransferHistoryQuery;
import ksh.tryptobackend.wallet.domain.vo.TransferType;

public record FindTransferHistoryRequest(
        TransferType type,
        Long cursorTransferId,
        @Min(1) @Max(50) Integer size) {

    public FindTransferHistoryQuery toQuery(Long walletId, Long userId) {
        return new FindTransferHistoryQuery(walletId, userId, type, cursorTransferId, size);
    }
}
