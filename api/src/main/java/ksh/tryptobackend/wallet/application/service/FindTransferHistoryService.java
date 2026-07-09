package ksh.tryptobackend.wallet.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.wallet.application.port.in.FindTransferHistoryUseCase;
import ksh.tryptobackend.wallet.application.port.in.GetWalletOwnerIdUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.query.FindTransferHistoryQuery;
import ksh.tryptobackend.wallet.application.port.in.dto.result.TransferHistoryCursorResult;
import ksh.tryptobackend.wallet.application.port.in.dto.result.TransferHistoryResult;
import ksh.tryptobackend.wallet.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.wallet.application.port.out.TransferQueryPort;
import ksh.tryptobackend.wallet.domain.model.Transfer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindTransferHistoryService implements FindTransferHistoryUseCase {

    private final TransferQueryPort transferQueryPort;
    private final MarketDataQueryPort marketDataQueryPort;

    private final GetWalletOwnerIdUseCase getWalletOwnerIdUseCase;

    @Override
    @Transactional(readOnly = true)
    public TransferHistoryCursorResult findTransferHistory(FindTransferHistoryQuery query) {
        validateWalletOwnership(query.walletId(), query.userId());

        List<Transfer> transfers = fetchTransfersWithOverflow(query);
        boolean hasNext = transfers.size() > query.size();
        List<Transfer> pagedTransfers = hasNext ? transfers.subList(0, query.size()) : transfers;

        return buildCursorResult(pagedTransfers, query.walletId(), hasNext);
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

    private Map<Long, String> resolveCoinSymbols(List<Transfer> transfers) {
        Set<Long> coinIds = transfers.stream().map(Transfer::getCoinId).collect(Collectors.toSet());
        return marketDataQueryPort.findCoinSymbols(coinIds);
    }

    private TransferHistoryCursorResult buildCursorResult(
            List<Transfer> transfers, Long viewerWalletId, boolean hasNext) {
        Map<Long, String> coinSymbols = resolveCoinSymbols(transfers);
        List<TransferHistoryResult> content =
                transfers.stream()
                        .map(
                                transfer ->
                                        TransferHistoryResult.from(
                                                transfer, viewerWalletId, coinSymbols))
                        .toList();
        Long nextCursor = hasNext ? transfers.getLast().getTransferId() : null;
        return new TransferHistoryCursorResult(content, nextCursor, hasNext);
    }
}
