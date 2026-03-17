package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.FilledOrderCounts;
import ksh.tryptobackend.trading.domain.vo.FilledOrder;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.Side;

import ksh.tryptobackend.trading.domain.vo.PendingOrder;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderQueryPort {

    List<PendingOrder> findAllPendingOrders();

    List<Order> findByCursor(Long walletId, Long exchangeCoinId, Side side,
                             OrderStatus status, Long cursorOrderId, int size);

    List<FilledOrder> findFilledByOrderIds(List<Long> orderIds);

    List<FilledOrder> findFilledSellOrders(Long walletId, Long exchangeCoinId, LocalDateTime after);

    boolean existsFilledByWalletId(Long walletId);

    int countFilledByWalletId(Long walletId);

    FilledOrderCounts countFilledGroupByWalletId(List<Long> walletIds);
}
