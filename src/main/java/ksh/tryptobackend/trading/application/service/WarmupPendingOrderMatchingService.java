package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.WarmupPendingOrderMatchingUseCase;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.application.port.out.PendingOrderCacheCommandPort;
import ksh.tryptobackend.trading.domain.vo.PendingOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarmupPendingOrderMatchingService implements WarmupPendingOrderMatchingUseCase {

    private final PendingOrderCacheCommandPort pendingOrderCacheCommandPort;
    private final OrderQueryPort orderQueryPort;

    @Override
    public void warmup() {
        List<PendingOrder> pendingOrders = orderQueryPort.findAllPendingOrders();
        pendingOrderCacheCommandPort.addAll(pendingOrders);
        log.info("미체결 주문 캐시 로딩 완료: {}건", pendingOrders.size());
    }
}
