package ksh.tryptobackend.wallet.adapter.in.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.wallet.application.port.in.dto.result.TransferHistoryResult;
import ksh.tryptobackend.wallet.domain.vo.TransferStatus;
import ksh.tryptobackend.wallet.domain.vo.TransferType;

public record TransferHistoryResponse(
        Long transferId,
        TransferType type,
        Long coinId,
        String coinSymbol,
        BigDecimal amount,
        TransferStatus status,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {

    public static TransferHistoryResponse from(TransferHistoryResult result) {
        return new TransferHistoryResponse(
                result.transferId(),
                result.type(),
                result.coinId(),
                result.coinSymbol(),
                result.amount(),
                result.status(),
                result.createdAt(),
                result.completedAt());
    }
}
