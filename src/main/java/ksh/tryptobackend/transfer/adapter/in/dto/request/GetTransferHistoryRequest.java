package ksh.tryptobackend.transfer.adapter.in.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import ksh.tryptobackend.transfer.application.port.in.dto.query.GetTransferHistoryQuery;
import ksh.tryptobackend.transfer.domain.vo.TransferType;

public record GetTransferHistoryRequest(
    TransferType type,
    Long cursor,
    @Min(1) @Max(50) Integer size
) {

    public GetTransferHistoryRequest {
        if (type == null) {
            type = TransferType.ALL;
        }
        if (size == null) {
            size = 20;
        }
    }

    public GetTransferHistoryQuery toQuery(Long walletId) {
        return new GetTransferHistoryQuery(walletId, type, cursor, size);
    }
}
