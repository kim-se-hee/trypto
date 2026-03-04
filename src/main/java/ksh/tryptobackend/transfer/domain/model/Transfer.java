package ksh.tryptobackend.transfer.domain.model;

import ksh.tryptobackend.transfer.domain.vo.TransferBalanceChange;
import ksh.tryptobackend.transfer.domain.vo.TransferDestination;
import ksh.tryptobackend.transfer.domain.vo.TransferFailureReason;
import ksh.tryptobackend.transfer.domain.vo.TransferStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    public static Transfer create(UUID idempotencyKey, Long fromWalletId,
                                   Long coinId, String chain, String toAddress, String toTag,
                                   BigDecimal amount, BigDecimal fee,
                                   TransferDestination destination, LocalDateTime createdAt) {
        return switch (destination) {
            case TransferDestination.Resolved r ->
                success(idempotencyKey, fromWalletId, r.walletId(),
                    coinId, chain, toAddress, toTag, amount, fee, createdAt);
            case TransferDestination.Failed f ->
                frozen(idempotencyKey, fromWalletId,
                    coinId, chain, toAddress, toTag, amount, fee, f.reason(), createdAt);
        };
    }

    public void refund() {
        this.status = TransferStatus.REFUNDED;
    }

    public BigDecimal getTotalDeduction() {
        return amount.add(fee);
    }

    public List<TransferBalanceChange> planBalanceChanges() {
        BigDecimal totalDeduction = getTotalDeduction();
        return switch (status) {
            case SUCCESS -> List.of(
                new TransferBalanceChange.Deduct(fromWalletId, coinId, totalDeduction),
                new TransferBalanceChange.Add(toWalletId, coinId, amount)
            );
            case FROZEN -> List.of(
                new TransferBalanceChange.Lock(fromWalletId, coinId, totalDeduction)
            );
            default -> throw new IllegalStateException("지원하지 않는 송금 상태: " + status);
        };
    }

    private static Transfer success(UUID idempotencyKey, Long fromWalletId, Long toWalletId,
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

    private static Transfer frozen(UUID idempotencyKey, Long fromWalletId,
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
}
