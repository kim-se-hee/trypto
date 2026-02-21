package ksh.tryptobackend.trading.application.port.in;

import ksh.tryptobackend.common.dto.response.CursorPageResponseDto;
import ksh.tryptobackend.trading.application.port.in.dto.query.FindOrderHistoryQuery;
import ksh.tryptobackend.trading.application.port.in.dto.result.OrderHistoryResult;

public interface FindOrderHistoryUseCase {

    CursorPageResponseDto<OrderHistoryResult> findOrderHistory(FindOrderHistoryQuery query);
}
