package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.application.port.out.dto.OrderInfo;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderQueryPort {

    List<OrderInfo> findFilledByOrderIds(List<Long> orderIds);

    List<OrderInfo> findFilledSellOrders(Long walletId, Long exchangeCoinId, LocalDateTime after);

    boolean existsFilledByWalletId(Long walletId);

    int countFilledByWalletId(Long walletId);
}
