package ksh.tryptobackend.wallet.application.port.in.dto.result;

import java.util.List;
import java.util.Map;
import ksh.tryptobackend.wallet.domain.model.Transfer;

public record TransferHistoryCursorResult(
        List<Transfer> transfers,
        Map<Long, String> coinSymbolMap,
        Long nextCursor,
        boolean hasNext) {}
