package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.out.OrderCommandPort;
import ksh.tryptobackend.trading.application.port.out.OrderFillFailureCommandPort;
import ksh.tryptobackend.trading.application.port.out.OrderFillFailureQueryPort;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.model.OrderFillFailure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompensateFillFailureService {

    private final OrderFillFailureQueryPort orderFillFailureQueryPort;
    private final OrderFillFailureCommandPort orderFillFailureCommandPort;
    private final OrderCommandPort orderCommandPort;
    private final MatchPendingOrdersService matchPendingOrdersService;

    @Scheduled(fixedDelay = 60_000)
    public void compensate() {
        List<OrderFillFailure> failures = orderFillFailureQueryPort.findUnresolved();
        if (failures.isEmpty()) {
            return;
        }

        log.info("보상 스케줄러 실행: 미해결 실패 이력 {}건", failures.size());
        for (OrderFillFailure failure : failures) {
            processFailure(failure);
        }
    }

    private void processFailure(OrderFillFailure failure) {
        try {
            Order order = orderCommandPort.findById(failure.getOrderId()).orElse(null);
            if (order == null || !order.isPending()) {
                resolveFailure(failure);
                return;
            }
            matchPendingOrdersService.fillOrder(failure.getOrderId(), failure.getAttemptedPrice());
            resolveFailure(failure);
        } catch (Exception e) {
            log.warn("보상 체결 실패: orderId={}", failure.getOrderId(), e);
        }
    }

    private void resolveFailure(OrderFillFailure failure) {
        failure.resolve();
        orderFillFailureCommandPort.save(failure);
    }
}
