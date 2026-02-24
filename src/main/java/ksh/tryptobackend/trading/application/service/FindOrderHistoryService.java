package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.common.dto.response.CursorPageResponseDto;
import ksh.tryptobackend.trading.application.port.in.FindOrderHistoryUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.query.FindOrderHistoryQuery;
import ksh.tryptobackend.trading.application.port.in.dto.result.OrderHistoryResult;
import ksh.tryptobackend.trading.application.port.out.OrderPersistencePort;
import ksh.tryptobackend.trading.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindOrderHistoryService implements FindOrderHistoryUseCase {

    private final OrderPersistencePort orderPersistencePort;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponseDto<OrderHistoryResult> findOrderHistory(FindOrderHistoryQuery query) {
        List<Order> orders = orderPersistencePort.findByCursor(
            query.walletId(), query.exchangeCoinId(), query.side(),
            query.status(), query.cursorOrderId(), query.size() + 1);

        boolean hasNext = orders.size() > query.size();
        List<Order> result = hasNext ? orders.subList(0, query.size()) : orders;

        List<OrderHistoryResult> content = result.stream()
            .map(OrderHistoryResult::from)
            .toList();

        Long nextCursor = hasNext ? result.getLast().getId() : null;

        return CursorPageResponseDto.of(content, nextCursor, hasNext);
    }
}
