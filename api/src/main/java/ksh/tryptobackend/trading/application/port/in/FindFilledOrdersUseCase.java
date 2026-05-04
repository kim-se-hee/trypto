package ksh.tryptobackend.trading.application.port.in;

import java.time.LocalDateTime;
import java.util.List;
import ksh.tryptobackend.trading.application.port.in.dto.result.FilledOrderResult;

public interface FindFilledOrdersUseCase {

    List<FilledOrderResult> findByOrderIds(List<Long> orderIds);

    List<FilledOrderResult> findSellOrders(Long walletId, Long exchangeCoinId, LocalDateTime after);
}
