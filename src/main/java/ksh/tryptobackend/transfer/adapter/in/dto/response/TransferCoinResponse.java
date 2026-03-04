package ksh.tryptobackend.transfer.adapter.in.dto.response;

import ksh.tryptobackend.transfer.domain.model.Transfer;
import ksh.tryptobackend.transfer.domain.vo.TransferFailureReason;
import ksh.tryptobackend.transfer.domain.vo.TransferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferCoinResponse(
    Long transferId,
    UUID idempotencyKey,
    Long fromWalletId,
    Long toWalletId,
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

    public static TransferCoinResponse from(Transfer transfer) {
        return new TransferCoinResponse(
            transfer.getTransferId(),
            transfer.getIdempotencyKey(),
            transfer.getFromWalletId(),
            transfer.getToWalletId(),
            transfer.getCoinId(),
            transfer.getChain(),
            transfer.getToAddress(),
            transfer.getToTag(),
            transfer.getAmount(),
            transfer.getFee(),
            transfer.getStatus(),
            transfer.getFailureReason(),
            transfer.getFrozenUntil(),
            transfer.getCreatedAt()
        );
    }
}
