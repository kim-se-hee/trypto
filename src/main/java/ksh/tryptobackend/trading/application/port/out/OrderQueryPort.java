package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.application.port.out.dto.OrderInfo;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.Side;

import ksh.tryptobackend.trading.domain.vo.FilledOrderCounts;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderQueryPort {

    List<Order> findByCursor(Long walletId, Long exchangeCoinId, Side side,
                             OrderStatus status, Long cursorOrderId, int size);

    List<OrderInfo> findFilledByOrderIds(List<Long> orderIds);

    List<OrderInfo> findFilledSellOrders(Long walletId, Long exchangeCoinId, LocalDateTime after);

    boolean existsFilledByWalletId(Long walletId);

    int countFilledByWalletId(Long walletId);

    FilledOrderCounts countFilledGroupByWalletId(List<Long> walletIds);
}
