package ksh.tryptobackend.wallet.application.port.in.dto.result;

import java.util.List;

public record TransferHistoryCursorResult(List<TransferHistoryResult> content, Long nextCursor, boolean hasNext) {}
