package ksh.tryptobackend.transfer.domain.model;

import ksh.tryptobackend.transfer.domain.vo.TransferFailureReason;
import ksh.tryptobackend.transfer.domain.vo.TransferStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class Transfer {

    private static final long FROZEN_HOURS = 24;

    private final Long transferId;
    private final UUID idempotencyKey;
    private final Long fromWalletId;
    private final Long toWalletId;
    private final Long coinId;
    private final String chain;
    private final String toAddress;
    private final String toTag;
    private final BigDecimal amount;
    private final BigDecimal fee;
    private TransferStatus status;
    private final TransferFailureReason failureReason;
    private final LocalDateTime frozenUntil;
    private final LocalDateTime createdAt;

    public static Transfer success(UUID idempotencyKey, Long fromWalletId, Long toWalletId,
                                   Long coinId, String chain, String toAddress, String toTag,
                                   BigDecimal amount, BigDecimal fee, LocalDateTime createdAt) {
        return Transfer.builder()
            .idempotencyKey(idempotencyKey)
            .fromWalletId(fromWalletId)
            .toWalletId(toWalletId)
            .coinId(coinId)
            .chain(chain)
            .toAddress(toAddress)
            .toTag(toTag)
            .amount(amount)
            .fee(fee)
            .status(TransferStatus.SUCCESS)
            .createdAt(createdAt)
            .build();
    }

    public static Transfer frozen(UUID idempotencyKey, Long fromWalletId,
                                  Long coinId, String chain, String toAddress, String toTag,
                                  BigDecimal amount, BigDecimal fee,
                                  TransferFailureReason failureReason, LocalDateTime createdAt) {
        return Transfer.builder()
            .idempotencyKey(idempotencyKey)
            .fromWalletId(fromWalletId)
            .coinId(coinId)
            .chain(chain)
            .toAddress(toAddress)
            .toTag(toTag)
            .amount(amount)
            .fee(fee)
            .status(TransferStatus.FROZEN)
            .failureReason(failureReason)
            .frozenUntil(createdAt.plusHours(FROZEN_HOURS))
            .createdAt(createdAt)
            .build();
    }

    public void refund() {
        this.status = TransferStatus.REFUNDED;
    }
}
