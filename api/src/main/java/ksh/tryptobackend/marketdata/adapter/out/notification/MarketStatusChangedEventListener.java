package ksh.tryptobackend.marketdata.adapter.out.notification;

import ksh.tryptobackend.marketdata.application.port.out.MarketStatusNotificationPort;
import ksh.tryptobackend.marketdata.domain.event.MarketStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketStatusChangedEventListener {

    private final MarketStatusNotificationPort marketStatusNotificationPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMarketStatusChanged(MarketStatusChangedEvent event) {
        try {
            marketStatusNotificationPort.broadcast(event.toNotification());
        } catch (Exception e) {
            log.warn("상장 상태 변화 알림 발송 실패: {}", event, e);
        }
    }
}
