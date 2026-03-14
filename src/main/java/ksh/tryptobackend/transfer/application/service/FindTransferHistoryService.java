package ksh.tryptobackend.transfer.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.transfer.application.port.in.FindTransferHistoryUseCase;
import ksh.tryptobackend.transfer.application.port.in.dto.query.FindTransferHistoryQuery;
import ksh.tryptobackend.transfer.application.port.in.dto.result.TransferHistoryCursorResult;
import ksh.tryptobackend.transfer.application.port.out.TransferQueryPort;
import ksh.tryptobackend.transfer.domain.model.Transfer;
import ksh.tryptobackend.wallet.application.port.in.GetWalletOwnerIdUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindTransferHistoryService implements FindTransferHistoryUseCase {

    private final GetWalletOwnerIdUseCase getWalletOwnerIdUseCase;
    private final TransferQueryPort transferQueryPort;

    @Override
    @Transactional(readOnly = true)
    public TransferHistoryCursorResult findTransferHistory(FindTransferHistoryQuery query) {
        validateWalletOwnership(query.walletId(), query.userId());

        List<Transfer> transfers = fetchTransfersWithOverflow(query);
        boolean hasNext = transfers.size() > query.size();
        List<Transfer> pagedTransfers = hasNext ? transfers.subList(0, query.size()) : transfers;

        return buildCursorResult(pagedTransfers, hasNext);
    }

    private void validateWalletOwnership(Long walletId, Long userId) {
        Long ownerUserId = getWalletOwnerIdUseCase.getWalletOwnerId(walletId);
        if (!ownerUserId.equals(userId)) {
            throw new CustomException(ErrorCode.WALLET_ACCESS_DENIED);
        }
    }

    private List<Transfer> fetchTransfersWithOverflow(FindTransferHistoryQuery query) {
        return transferQueryPort.findByCursor(
            query.walletId(), query.type(), query.cursorTransferId(), query.size() + 1);
    }

    private TransferHistoryCursorResult buildCursorResult(List<Transfer> transfers, boolean hasNext) {
        Long nextCursor = hasNext ? transfers.getLast().getTransferId() : null;
        return new TransferHistoryCursorResult(transfers, nextCursor, hasNext);
    }
}
