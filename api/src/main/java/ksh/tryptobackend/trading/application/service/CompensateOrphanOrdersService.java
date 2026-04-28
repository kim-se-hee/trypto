package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.marketdata.application.port.in.FindTicksUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.TickResult;
import ksh.tryptobackend.trading.application.port.in.CompensateOrphanOrdersUseCase;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.domain.service.OrphanOrderCompensator;
import ksh.tryptobackend.trading.domain.vo.OrphanOrder;
import ksh.tryptobackend.trading.domain.vo.PriceCandidate;
import ksh.tryptobackend.trading.domain.vo.PriceCandidates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompensateOrphanOrdersService implements CompensateOrphanOrdersUseCase {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final OrderQueryPort orderQueryPort;

    private final FindTicksUseCase findTicksUseCase;

    private final OrphanOrderCompensator orphanOrderCompensator;

    private final Clock clock;

    @Override
    public int compensate(int boundarySeconds) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<OrphanOrder> orphans = orderQueryPort.findOrphanOrders(now.minusSeconds(boundarySeconds));
        if (orphans.isEmpty()) {
            return 0;
        }

        Instant nowInstant = now.atZone(KST).toInstant();

        int filled = 0;
        for (OrphanOrder orphan : orphans) {
            if (compensateOne(orphan, nowInstant)) {
                filled++;
            }
        }
        return filled;
    }

    private boolean compensateOne(OrphanOrder orphan, Instant nowInstant) {
        try {
            Instant from = orphan.createdAt().atZone(KST).toInstant();
            List<TickResult> ticks = findTicksUseCase.findTicks(
                orphan.exchangeName(), orphan.marketSymbol(), from, nowInstant);
            PriceCandidates candidates = toPriceCandidates(ticks);

            Optional<PriceCandidate> matchedPrice = candidates.findFirstMatching(orphan);
            return matchedPrice
                .map(c -> orphanOrderCompensator.compensate(orphan, c))
                .orElse(false);
        } catch (Exception e) {
            log.error("orphan {} 보상 실패", orphan.orderId(), e);
            return false;
        }
    }

    private PriceCandidates toPriceCandidates(List<TickResult> ticks) {
        return new PriceCandidates(ticks.stream()
            .map(t -> new PriceCandidate(t.time(), t.price()))
            .toList());
    }
}
