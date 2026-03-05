package ksh.tryptobackend.transfer.adapter.in.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.transfer.application.port.in.dto.query.FindTransferHistoryQuery;
import ksh.tryptobackend.transfer.domain.vo.TransferType;

public record FindTransferHistoryRequest(
    @NotNull Long userId,
    TransferType type,
    Long cursorTransferId,
    @Min(1) @Max(50) Integer size
) {

    public FindTransferHistoryQuery toQuery(Long walletId) {
        return new FindTransferHistoryQuery(walletId, userId, type, cursorTransferId, size);
    }
}
