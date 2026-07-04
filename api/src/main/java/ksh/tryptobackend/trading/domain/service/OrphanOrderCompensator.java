package ksh.tryptobackend.trading.domain.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import ksh.tryptobackend.trading.application.port.in.RecalculateHoldingUseCase;
import ksh.tryptobackend.trading.application.port.out.MarketQueryPort;
import ksh.tryptobackend.trading.application.port.out.OrderCommandPort;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.OrphanOrder;
import ksh.tryptobackend.trading.domain.vo.PriceCandidate;
import ksh.tryptobackend.trading.domain.vo.TradingPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanOrderCompensator {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final OrderQueryPort orderQueryPort;
    private final OrderCommandPort orderCommandPort;
    private final MarketQueryPort marketQueryPort;

    private final WalletBalanceService walletBalanceService;
    private final RecalculateHoldingUseCase recalculateHoldingUseCase;

    @Transactional
    public boolean compensate(OrphanOrder orphan, PriceCandidate match) {
        Order order = orderQueryPort.getByIdWithLock(orphan.orderId());
        if (!order.isPending()) {
            return false;
        }

        LocalDateTime filledAt = LocalDateTime.ofInstant(match.time(), KST);
        order.fill(match.price(), filledAt);

        TradingPair pair = marketQueryPort.getTradingPair(order.getExchangeCoinId());
        walletBalanceService.applyAll(order.getWalletId(), order.planSettlementChanges(pair));
        orderCommandPort.save(order);
        recalculateHoldingUseCase.recalculate(order.getWalletId(), order.getExchangeCoinId());
        log.info("orphan {} 보상 완료 at {}", orphan.orderId(), match.price());
        return true;
    }
}
