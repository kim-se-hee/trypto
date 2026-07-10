package ksh.tryptobackend.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.wallet.application.port.in.dto.command.TransferCoinCommand;
import ksh.tryptobackend.wallet.domain.vo.TransferStatus;
import ksh.tryptobackend.wallet.domain.vo.TransferType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Transfer {

    private final Long transferId;
    private final Long fromWalletId;
    private final Long toWalletId;
    private final Long coinId;
    private final BigDecimal amount;
    private final TransferStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime completedAt;

    public static Transfer create(TransferCoinCommand command, LocalDateTime createdAt) {
        validateDifferentWallet(command.fromWalletId(), command.toWalletId());
        return Transfer.builder()
                .fromWalletId(command.fromWalletId())
                .toWalletId(command.toWalletId())
                .coinId(command.coinId())
                .amount(command.amount())
                .status(TransferStatus.SUCCESS)
                .createdAt(createdAt)
                .completedAt(createdAt)
                .build();
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
