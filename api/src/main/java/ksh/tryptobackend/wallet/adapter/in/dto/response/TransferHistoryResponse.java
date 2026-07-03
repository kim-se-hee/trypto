package ksh.tryptobackend.wallet.adapter.in.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import ksh.tryptobackend.wallet.domain.model.Transfer;
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

    public static TransferHistoryResponse from(
            Transfer transfer, Long viewerWalletId, Map<Long, String> coinSymbolMap) {
        return new TransferHistoryResponse(
                transfer.getTransferId(),
                transfer.resolveType(viewerWalletId),
                transfer.getCoinId(),
                coinSymbolMap.get(transfer.getCoinId()),
                transfer.getAmount(),
                transfer.getStatus(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt());
    }
}
