package ksh.tryptobackend.wallet.adapter.in.dto.response;

import ksh.tryptobackend.wallet.domain.model.Transfer;
import ksh.tryptobackend.wallet.domain.vo.TransferStatus;

public record TransferCoinResponse(Long transferId, TransferStatus status) {

    public static TransferCoinResponse from(Transfer transfer) {
        return new TransferCoinResponse(transfer.getTransferId(), transfer.getStatus());
    }

    /** 멱등 재요청 응답. 송금은 검증을 통과하면 항상 SUCCESS 이므로 상태를 고정한다. */
    public static TransferCoinResponse duplicate(Long transferId) {
        return new TransferCoinResponse(transferId, TransferStatus.SUCCESS);
    }
}
