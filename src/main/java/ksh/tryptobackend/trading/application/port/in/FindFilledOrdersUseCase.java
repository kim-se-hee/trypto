package ksh.tryptobackend.trading.application.port.in;

import ksh.tryptobackend.trading.application.port.in.dto.result.FilledOrderResult;

import java.time.LocalDateTime;
import java.util.List;

public interface FindFilledOrdersUseCase {

    List<FilledOrderResult> findByOrderIds(List<Long> orderIds);

    List<FilledOrderResult> findSellOrders(Long walletId, Long exchangeCoinId, LocalDateTime after);
}
