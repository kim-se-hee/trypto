package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.application.port.out.dto.OrderInfo;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderHistoryPort {

    List<OrderInfo> findByOrderIds(List<Long> orderIds);

    List<OrderInfo> findSellOrdersAfter(Long walletId, Long exchangeCoinId, LocalDateTime after);
}
