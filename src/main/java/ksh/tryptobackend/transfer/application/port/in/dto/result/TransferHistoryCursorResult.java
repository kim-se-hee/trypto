package ksh.tryptobackend.transfer.application.port.in.dto.result;

import ksh.tryptobackend.transfer.domain.model.Transfer;

import java.util.List;

public record TransferHistoryCursorResult(
    List<Transfer> transfers,
    Long nextCursor,
    boolean hasNext
) {
}
