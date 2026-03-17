package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.FillPendingOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.MatchPendingOrdersUseCase;
import ksh.tryptobackend.trading.application.port.out.ExchangeCoinMappingCacheQueryPort;
import ksh.tryptobackend.trading.application.port.out.OrderFillFailureCommandPort;
import ksh.tryptobackend.trading.application.port.out.PendingOrderCacheCommandPort;
import ksh.tryptobackend.trading.application.port.out.PendingOrderCacheQueryPort;
import ksh.tryptobackend.trading.domain.model.OrderFillFailure;
import ksh.tryptobackend.trading.domain.vo.PendingOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchPendingOrdersService implements MatchPendingOrdersUseCase {

    private static final int MAX_RETRY_COUNT = 2;
    private static final long[] RETRY_BACKOFF_MS = {50, 100};

    private final PendingOrderCacheCommandPort pendingOrderCacheCommandPort;
    private final PendingOrderCacheQueryPort pendingOrderCacheQueryPort;
    private final ExchangeCoinMappingCacheQueryPort exchangeCoinMappingCacheQueryPort;
    private final OrderFillFailureCommandPort orderFillFailureCommandPort;

    private final FillPendingOrderUseCase fillPendingOrderUseCase;

    private final Clock clock;

    @Override
    public void matchOrders(String exchange, String symbol, BigDecimal currentPrice) {
        Long exchangeCoinId = resolveExchangeCoinId(exchange, symbol);
        if (exchangeCoinId == null) {
            return;
        }

        List<PendingOrder> matchedOrders = pendingOrderCacheQueryPort
            .findMatchedOrders(exchangeCoinId, currentPrice);
        if (matchedOrders.isEmpty()) {
            return;
        }

        for (PendingOrder pendingOrder : matchedOrders) {
            pendingOrderCacheCommandPort.remove(exchangeCoinId, pendingOrder.orderId());
            processFillWithRetry(pendingOrder, currentPrice);
        }
    }

    private Long resolveExchangeCoinId(String exchange, String symbol) {
        return exchangeCoinMappingCacheQueryPort.resolve(exchange, symbol)
            .orElseGet(() -> {
                log.warn("매핑 변환 실패: exchange={}, symbol={}", exchange, symbol);
                return null;
            });
    }

    private void processFillWithRetry(PendingOrder pendingOrder, BigDecimal currentPrice) {
        for (int attempt = 0; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                fillPendingOrderUseCase.fillOrder(pendingOrder.orderId(), currentPrice);
                return;
            } catch (OptimisticLockingFailureException e) {
                log.info("낙관적 락 충돌 (취소/다른 서버 매칭): orderId={}", pendingOrder.orderId());
                return;
            } catch (Exception e) {
                if (attempt < MAX_RETRY_COUNT) {
                    sleepForRetry(attempt);
                    log.warn("체결 처리 재시도 ({}/{}): orderId={}", attempt + 1, MAX_RETRY_COUNT, pendingOrder.orderId());
                } else {
                    handleFillFailure(pendingOrder, currentPrice, e);
                }
            }
        }
    }

    private void sleepForRetry(int attempt) {
        try {
            Thread.sleep(RETRY_BACKOFF_MS[attempt]);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleFillFailure(PendingOrder pendingOrder, BigDecimal currentPrice, Exception e) {
        log.error("체결 처리 실패 (재시도 소진): orderId={}", pendingOrder.orderId(), e);
        pendingOrderCacheCommandPort.add(pendingOrder);
        recordFillFailure(pendingOrder, currentPrice, e);
    }

    private void recordFillFailure(PendingOrder pendingOrder, BigDecimal attemptedPrice, Exception e) {
        try {
            OrderFillFailure failure = OrderFillFailure.create(
                pendingOrder.orderId(), attemptedPrice,
                LocalDateTime.now(clock), e.getMessage());
            orderFillFailureCommandPort.save(failure);
        } catch (Exception saveEx) {
            log.error("체결 실패 이력 저장 실패: orderId={}", pendingOrder.orderId(), saveEx);
        }
    }
}
