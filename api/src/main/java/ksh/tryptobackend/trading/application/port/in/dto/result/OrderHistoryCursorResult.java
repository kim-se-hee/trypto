package ksh.tryptobackend.trading.application.port.in.dto.result;

import java.util.List;

public record OrderHistoryCursorResult(List<OrderHistoryResult> content, Long nextCursor, boolean hasNext) {}
