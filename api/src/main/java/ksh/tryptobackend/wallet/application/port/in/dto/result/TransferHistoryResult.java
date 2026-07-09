package ksh.tryptobackend.wallet.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import ksh.tryptobackend.wallet.domain.model.Transfer;
import ksh.tryptobackend.wallet.domain.vo.TransferStatus;
import ksh.tryptobackend.wallet.domain.vo.TransferType;

public record TransferHistoryResult(
        Long transferId,
        TransferType type,
        Long coinId,
        String coinSymbol,
        BigDecimal amount,
        TransferStatus status,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {

    public static TransferHistoryResult from(
            Transfer transfer, Long viewerWalletId, Map<Long, String> coinSymbols) {
        return new TransferHistoryResult(
                transfer.getTransferId(),
                transfer.resolveType(viewerWalletId),
                transfer.getCoinId(),
                coinSymbols.get(transfer.getCoinId()),
                transfer.getAmount(),
                transfer.getStatus(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt());
    }
}
