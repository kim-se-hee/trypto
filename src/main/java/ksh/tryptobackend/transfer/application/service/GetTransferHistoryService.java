package ksh.tryptobackend.transfer.application.service;

import ksh.tryptobackend.transfer.application.port.in.GetTransferHistoryUseCase;
import ksh.tryptobackend.transfer.application.port.in.dto.query.GetTransferHistoryQuery;
import ksh.tryptobackend.transfer.application.port.in.dto.result.TransferHistoryCursorResult;
import ksh.tryptobackend.transfer.application.port.out.TransferPersistencePort;
import ksh.tryptobackend.transfer.application.port.out.TransferWalletPort;
import ksh.tryptobackend.transfer.domain.model.Transfer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetTransferHistoryService implements GetTransferHistoryUseCase {

    private final TransferWalletPort transferWalletPort;
    private final TransferPersistencePort transferPersistencePort;

    @Override
    @Transactional(readOnly = true)
    public TransferHistoryCursorResult getTransferHistory(GetTransferHistoryQuery query) {
        transferWalletPort.getWallet(query.walletId());

        List<Transfer> transfers = fetchTransfersWithOverflow(query);
        boolean hasNext = transfers.size() > query.size();
        List<Transfer> trimmed = hasNext ? transfers.subList(0, query.size()) : transfers;

        return buildCursorResult(trimmed, hasNext);
    }

    private List<Transfer> fetchTransfersWithOverflow(GetTransferHistoryQuery query) {
        return transferPersistencePort.findByCursor(
            query.walletId(), query.type(), query.cursorTransferId(), query.size() + 1);
    }

    private TransferHistoryCursorResult buildCursorResult(List<Transfer> transfers, boolean hasNext) {
        Long nextCursor = hasNext ? transfers.getLast().getTransferId() : null;
        return new TransferHistoryCursorResult(transfers, nextCursor, hasNext);
    }
}
