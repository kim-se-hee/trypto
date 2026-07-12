package ksh.tryptobackend.wallet.adapter.in.dto.response;

import ksh.tryptobackend.wallet.domain.model.Transfer;
import ksh.tryptobackend.wallet.domain.vo.TransferStatus;

public record TransferCoinResponse(Long transferId, TransferStatus status) {

    public static TransferCoinResponse from(Transfer transfer) {
        return new TransferCoinResponse(transfer.getTransferId(), transfer.getStatus());
    }

    public static TransferCoinResponse duplicate(Long transferId) {
        return new TransferCoinResponse(transferId, TransferStatus.SUCCESS);
    }
}
