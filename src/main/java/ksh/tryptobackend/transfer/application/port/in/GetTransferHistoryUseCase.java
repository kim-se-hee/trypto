package ksh.tryptobackend.transfer.application.port.in;

import ksh.tryptobackend.transfer.application.port.in.dto.query.GetTransferHistoryQuery;
import ksh.tryptobackend.transfer.application.port.in.dto.result.TransferHistoryCursorResult;

public interface GetTransferHistoryUseCase {

    TransferHistoryCursorResult getTransferHistory(GetTransferHistoryQuery query);
}
