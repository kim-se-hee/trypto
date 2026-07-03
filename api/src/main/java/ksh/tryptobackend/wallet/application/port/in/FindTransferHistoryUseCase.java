package ksh.tryptobackend.wallet.application.port.in;

import ksh.tryptobackend.wallet.application.port.in.dto.query.FindTransferHistoryQuery;
import ksh.tryptobackend.wallet.application.port.in.dto.result.TransferHistoryCursorResult;

public interface FindTransferHistoryUseCase {

    TransferHistoryCursorResult findTransferHistory(FindTransferHistoryQuery query);
}
