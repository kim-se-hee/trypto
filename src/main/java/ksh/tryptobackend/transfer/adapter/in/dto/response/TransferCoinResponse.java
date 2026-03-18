package ksh.tryptobackend.transfer.adapter.in.dto.response;

import ksh.tryptobackend.transfer.domain.model.Transfer;
import ksh.tryptobackend.transfer.domain.vo.TransferStatus;

public record TransferCoinResponse(
    Long transferId,
    TransferStatus status
) {

    public static TransferCoinResponse from(Transfer transfer) {
        return new TransferCoinResponse(
            transfer.getTransferId(),
            transfer.getStatus()
        );
    }
}
