package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.FindOrderHistoryUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.query.FindOrderHistoryQuery;
import ksh.tryptobackend.trading.application.port.in.dto.result.OrderHistoryCursorResult;
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
    public OrderHistoryCursorResult findOrderHistory(FindOrderHistoryQuery query) {
        List<Order> orders = fetchOrdersWithOverflow(query);
        boolean hasNext = orders.size() > query.size();
        List<Order> trimmed = hasNext ? orders.subList(0, query.size()) : orders;

        return buildCursorResult(trimmed, hasNext);
    }

    private List<Order> fetchOrdersWithOverflow(FindOrderHistoryQuery query) {
        return orderPersistencePort.findByCursor(
            query.walletId(), query.exchangeCoinId(), query.side(),
            query.status(), query.cursorOrderId(), query.size() + 1);
    }

    private OrderHistoryCursorResult buildCursorResult(List<Order> orders, boolean hasNext) {
        List<OrderHistoryResult> content = orders.stream()
            .map(OrderHistoryResult::from)
            .toList();
        Long nextCursor = hasNext ? orders.getLast().getId() : null;

        return new OrderHistoryCursorResult(content, nextCursor, hasNext);
    }
}
