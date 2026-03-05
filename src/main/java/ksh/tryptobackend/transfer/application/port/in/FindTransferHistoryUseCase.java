package ksh.tryptobackend.transfer.application.port.in;

import ksh.tryptobackend.transfer.application.port.in.dto.query.FindTransferHistoryQuery;
import ksh.tryptobackend.transfer.application.port.in.dto.result.TransferHistoryCursorResult;

public interface FindTransferHistoryUseCase {

    TransferHistoryCursorResult findTransferHistory(FindTransferHistoryQuery query);
}
