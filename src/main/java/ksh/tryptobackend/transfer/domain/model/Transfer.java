package ksh.tryptobackend.transfer.domain.model;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.transfer.domain.vo.TransferBalanceChange;
import ksh.tryptobackend.transfer.domain.vo.TransferStatus;
import ksh.tryptobackend.transfer.domain.vo.TransferType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class Transfer {

    private final Long transferId;
    private final UUID idempotencyKey;
    private final Long fromWalletId;
    private final Long toWalletId;
    private final Long coinId;
    private final BigDecimal amount;
    private final TransferStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime completedAt;

    public static Transfer create(UUID idempotencyKey, Long fromWalletId, Long toWalletId,
                                   Long coinId, BigDecimal amount, LocalDateTime createdAt) {
        validateDifferentWallet(fromWalletId, toWalletId);
        return Transfer.builder()
            .idempotencyKey(idempotencyKey)
            .fromWalletId(fromWalletId)
            .toWalletId(toWalletId)
            .coinId(coinId)
            .amount(amount)
            .status(TransferStatus.SUCCESS)
            .createdAt(createdAt)
            .completedAt(createdAt)
            .build();
    }

    public BigDecimal getTotalDeduction() {
        return amount;
    }

    public List<TransferBalanceChange> planBalanceChanges() {
        return List.of(
            new TransferBalanceChange.Deduct(fromWalletId, coinId, amount),
            new TransferBalanceChange.Add(toWalletId, coinId, amount)
        );
    }

    public TransferType resolveType(Long viewerWalletId) {
        return fromWalletId.equals(viewerWalletId) ? TransferType.WITHDRAW : TransferType.DEPOSIT;
    }

    private static void validateDifferentWallet(Long fromWalletId, Long toWalletId) {
        if (fromWalletId.equals(toWalletId)) {
            throw new CustomException(ErrorCode.SAME_WALLET_TRANSFER);
        }
    }
}
