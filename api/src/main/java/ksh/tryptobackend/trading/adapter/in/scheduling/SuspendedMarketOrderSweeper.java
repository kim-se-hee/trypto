package ksh.tryptobackend.trading.adapter.in.scheduling;

import ksh.tryptobackend.trading.application.port.in.AutoCancelOrdersUseCase;
import ksh.tryptobackend.trading.application.port.in.FindSuspendedMarketsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuspendedMarketOrderSweeper {

    private final FindSuspendedMarketsUseCase findSuspendedMarketsUseCase;
    private final AutoCancelOrdersUseCase autoCancelOrdersUseCase;

    @Scheduled(fixedDelayString = "${trading.suspended-order-sweep-interval-ms:60000}")
    public void sweep() {
        for (Long exchangeCoinId : findSuspendedMarketsUseCase.findSuspendedMarkets()) {
            cancelSafely(exchangeCoinId);
        }
    }

    private void cancelSafely(Long exchangeCoinId) {
        try {
            autoCancelOrdersUseCase.autoCancel(exchangeCoinId);
        } catch (Exception e) {
            log.warn("거래지원 종료 마켓 미체결 주문 보정 취소 실패: exchangeCoinId={}", exchangeCoinId, e);
        }
    }
}
