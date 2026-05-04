package ksh.tryptobackend.transfer.application.port.in.dto.result;

import java.util.List;
import java.util.Map;
import ksh.tryptobackend.transfer.domain.model.Transfer;

public record TransferHistoryCursorResult(
        List<Transfer> transfers,
        Map<Long, String> coinSymbolMap,
        Long nextCursor,
        boolean hasNext) {}
