package ksh.tryptobackend.trading.application.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinMappingResult;
import ksh.tryptobackend.trading.application.port.in.FillPendingOrderUseCase;
import ksh.tryptobackend.trading.application.port.out.HoldingCommandPort;
import ksh.tryptobackend.trading.application.port.out.OrderCommandPort;
import ksh.tryptobackend.trading.application.port.out.OrderFilledEventPort;
import ksh.tryptobackend.trading.domain.model.Holding;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.OrderFilledEvent;
import ksh.tryptobackend.trading.domain.vo.TradingVenue;
import ksh.tryptobackend.wallet.application.port.in.GetWalletOwnerIdUseCase;
import ksh.tryptobackend.wallet.application.port.in.ManageWalletBalanceUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
public class FillPendingOrderService implements FillPendingOrderUseCase {

    private final OrderCommandPort orderCommandPort;
    private final HoldingCommandPort holdingCommandPort;

    private final FindExchangeCoinMappingUseCase findExchangeCoinMappingUseCase;
    private final FindExchangeDetailUseCase findExchangeDetailUseCase;

    private final ManageWalletBalanceUseCase manageWalletBalanceUseCase;
    private final GetWalletOwnerIdUseCase getWalletOwnerIdUseCase;

    private final OrderFilledEventPort orderFilledEventPort;
    private final Clock clock;

    private final Timer fillTimer;

    public FillPendingOrderService(OrderCommandPort orderCommandPort,
                                   HoldingCommandPort holdingCommandPort,
                                   FindExchangeCoinMappingUseCase findExchangeCoinMappingUseCase,
                                   FindExchangeDetailUseCase findExchangeDetailUseCase,
                                   ManageWalletBalanceUseCase manageWalletBalanceUseCase,
                                   GetWalletOwnerIdUseCase getWalletOwnerIdUseCase,
                                   OrderFilledEventPort orderFilledEventPort,
                                   Clock clock,
                                   MeterRegistry registry) {
        this.orderCommandPort = orderCommandPort;
        this.holdingCommandPort = holdingCommandPort;
        this.findExchangeCoinMappingUseCase = findExchangeCoinMappingUseCase;
        this.findExchangeDetailUseCase = findExchangeDetailUseCase;
        this.manageWalletBalanceUseCase = manageWalletBalanceUseCase;
        this.getWalletOwnerIdUseCase = getWalletOwnerIdUseCase;
        this.orderFilledEventPort = orderFilledEventPort;
        this.clock = clock;

        this.fillTimer = Timer.builder("pending.order.fill")
            .description("미체결 주문 체결 처리 시간 (단건)")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }

    @Override
    @Transactional
    public void fillOrder(Long orderId, BigDecimal currentPrice) {
        Timer.Sample sample = Timer.start();
        try {
            doFillOrder(orderId, currentPrice);
        } finally {
            sample.stop(fillTimer);
        }
    }

    private void doFillOrder(Long orderId, BigDecimal currentPrice) {
        boolean filled = orderCommandPort.fillOrder(orderId, LocalDateTime.now(clock));
        if (!filled) {
            log.info("주문 CAS 실패 (이미 처리됨): orderId={}", orderId);
            return;
        }

        Order order = orderCommandPort.findById(orderId)
            .orElseThrow(() -> new IllegalStateException("CAS 성공 후 조회 실패: orderId=" + orderId));

        ExchangeCoinMappingResult mapping = findExchangeCoinMapping(order.getExchangeCoinId());
        TradingVenue venue = getTradingVenue(mapping.exchangeId());

        settleBalance(order, mapping, venue);
        updateHolding(order, mapping, currentPrice);

        publishOrderFilledEvent(order, mapping);
    }

    private ExchangeCoinMappingResult findExchangeCoinMapping(Long exchangeCoinId) {
        return findExchangeCoinMappingUseCase.findById(exchangeCoinId)
            .orElseThrow(() -> new IllegalStateException("매핑 없음: exchangeCoinId=" + exchangeCoinId));
    }

    private TradingVenue getTradingVenue(Long exchangeId) {
        return findExchangeDetailUseCase.findExchangeDetail(exchangeId)
            .map(detail -> TradingVenue.of(detail.feeRate(), detail.baseCurrencyCoinId(), detail.domestic()))
            .orElseThrow(() -> new IllegalStateException("거래소 없음: exchangeId=" + exchangeId));
    }

    private void settleBalance(Order order, ExchangeCoinMappingResult mapping, TradingVenue venue) {
        if (order.isBuyOrder()) {
            settleBuyOrder(order, mapping, venue);
        } else {
            settleSellOrder(order, mapping, venue);
        }
    }

    private void settleBuyOrder(Order order, ExchangeCoinMappingResult mapping, TradingVenue venue) {
        Long baseCoinId = venue.baseCurrencyCoinId();
        BigDecimal unlockAmount = order.getSettlementDebit();

        manageWalletBalanceUseCase.unlockBalance(order.getWalletId(), baseCoinId, unlockAmount);
        manageWalletBalanceUseCase.deductBalance(order.getWalletId(), baseCoinId, unlockAmount);
        manageWalletBalanceUseCase.addBalance(order.getWalletId(), mapping.coinId(), order.getQuantity().value());
    }

    private void settleSellOrder(Order order, ExchangeCoinMappingResult mapping, TradingVenue venue) {
        BigDecimal unlockQuantity = order.getQuantity().value();

        manageWalletBalanceUseCase.unlockBalance(order.getWalletId(), mapping.coinId(), unlockQuantity);
        manageWalletBalanceUseCase.deductBalance(order.getWalletId(), mapping.coinId(), unlockQuantity);
        manageWalletBalanceUseCase.addBalance(order.getWalletId(), venue.baseCurrencyCoinId(), order.getSettlementCredit());
    }

    private void updateHolding(Order order, ExchangeCoinMappingResult mapping, BigDecimal currentPrice) {
        Holding holding = holdingCommandPort.findByWalletIdAndCoinId(order.getWalletId(), mapping.coinId())
            .orElseGet(() -> Holding.empty(order.getWalletId(), mapping.coinId()));
        holding.applyOrder(order.getSide(), order.getFilledPrice(), order.getQuantity().value(), currentPrice);
        holdingCommandPort.save(holding);
    }

    private void publishOrderFilledEvent(Order order, ExchangeCoinMappingResult mapping) {
        try {
            Long userId = getWalletOwnerIdUseCase.getWalletOwnerId(order.getWalletId());
            OrderFilledEvent event = new OrderFilledEvent(
                userId, order.getWalletId(), order.getId(), mapping.coinId(),
                order.getSide(), order.getQuantity().value(),
                order.getFilledPrice(), order.getFee().amount());
            orderFilledEventPort.publish(event);
        } catch (Exception e) {
            log.warn("체결 이벤트 발행 실패: orderId={}", order.getId(), e);
        }
    }
}
