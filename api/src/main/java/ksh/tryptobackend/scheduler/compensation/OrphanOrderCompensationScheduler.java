package ksh.tryptobackend.scheduler.compensation;

import ksh.tryptobackend.trading.application.port.in.CompensateOrphanOrdersUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanOrderCompensationScheduler {

    private final CompensateOrphanOrdersUseCase compensateOrphanOrdersUseCase;

    @Value("${compensation.boundary-seconds}")
    private int boundarySeconds;

    @Scheduled(fixedDelayString = "${compensation.interval-ms}")
    @SchedulerLock(
            name = "orphan-order-compensation",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT30S")
    public void compensateOrphanOrders() {
        int filled = compensateOrphanOrdersUseCase.compensate(boundarySeconds);
        if (filled > 0) {
            log.info("compensation filled {}", filled);
        }
    }
}
