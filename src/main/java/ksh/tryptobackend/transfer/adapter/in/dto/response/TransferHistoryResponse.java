package ksh.tryptobackend.transfer.adapter.in.dto.response;

import ksh.tryptobackend.transfer.domain.model.Transfer;
import ksh.tryptobackend.transfer.domain.vo.TransferFailureReason;
import ksh.tryptobackend.transfer.domain.vo.TransferStatus;
import ksh.tryptobackend.transfer.domain.vo.TransferType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferHistoryResponse(
    Long transferId,
    TransferType type,
    Long coinId,
    String chain,
    String toAddress,
    String toTag,
    BigDecimal amount,
    BigDecimal fee,
    TransferStatus status,
    TransferFailureReason failureReason,
    LocalDateTime frozenUntil,
    LocalDateTime createdAt
) {

    public static TransferHistoryResponse from(Transfer transfer, Long viewerWalletId) {
        return new TransferHistoryResponse(
            transfer.getTransferId(),
            transfer.resolveType(viewerWalletId),
            transfer.getCoinId(),
            transfer.getChain(),
            transfer.getToAddress(),
            transfer.getToTag(),
            transfer.getAmount(),
            transfer.resolveVisibleFee(viewerWalletId),
            transfer.getStatus(),
            transfer.getFailureReason(),
            transfer.getFrozenUntil(),
            transfer.getCreatedAt()
        );
    }
}
