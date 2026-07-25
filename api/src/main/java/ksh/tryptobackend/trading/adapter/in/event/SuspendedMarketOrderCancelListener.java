package ksh.tryptobackend.trading.adapter.in.event;

import ksh.tryptobackend.common.event.MarketSuspendedEvent;
import ksh.tryptobackend.trading.application.port.in.AutoCancelOrdersUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuspendedMarketOrderCancelListener {

    private final AutoCancelOrdersUseCase autoCancelOrdersUseCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMarketSuspended(MarketSuspendedEvent event) {
        try {
            autoCancelOrdersUseCase.autoCancel(event.exchangeCoinId());
        } catch (Exception e) {
            // 정지는 이미 커밋됐고 SuspendedMarketOrderSweeper 가 60초 내 회수를 보장하므로 예외를 전파해 재처리로 보내지 않는다
            log.warn("거래지원 종료 마켓 미체결 주문 자동 취소 실패: exchangeCoinId={}", event.exchangeCoinId(), e);
        }
    }
}
