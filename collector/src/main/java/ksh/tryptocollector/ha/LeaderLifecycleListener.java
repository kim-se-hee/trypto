package ksh.tryptocollector.ha;

import ksh.tryptocollector.backfill.CandleBackfillService;
import ksh.tryptocollector.ingest.ExchangeInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderLifecycleListener {

    private final ExchangeInitializer exchangeInitializer;
    private final CandleBackfillService candleBackfillService;

    @EventListener
    public void onAcquired(LeadershipAcquiredEvent event) {
        log.info("리더 활성화 시퀀스 시작");
        exchangeInitializer.start();
        candleBackfillService.triggerBackfill();
        log.info("리더 활성화 시퀀스 완료");
    }

    @EventListener
    public void onRevoked(LeadershipRevokedEvent event) {
        log.info("리더 비활성화 시퀀스 시작");
        exchangeInitializer.stop();
        log.info("리더 비활성화 시퀀스 완료");
    }
}
