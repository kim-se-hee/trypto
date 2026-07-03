package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.trading.application.port.in.SettleOrderUseCase;
import ksh.tryptobackend.trading.application.port.out.PositionCommandPort;
import ksh.tryptobackend.trading.domain.event.OrderFilledEvent;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.model.Position;
import ksh.tryptobackend.trading.domain.service.WalletBalanceService;
import ksh.tryptobackend.trading.domain.vo.ExecutedFill;
import ksh.tryptobackend.trading.domain.vo.Price;
import ksh.tryptobackend.trading.domain.vo.TradingPair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 체결된 주문을 정산한다 — 잠긴 잔고 소진/환불(wallet)과 보유 포지션 갱신을 한 트랜잭션으로 함께 반영한다. */
@Service
@RequiredArgsConstructor
public class SettleOrderService implements SettleOrderUseCase {

    private final PositionCommandPort positionCommandPort;
    private final WalletBalanceService walletBalanceService;

    @Override
    @Transactional
    public void settle(OrderFilledEvent event) {
        Order order = event.order();
        TradingPair pair = event.tradingPair();

        walletBalanceService.applyAll(order.getWalletId(), order.planSettlementChanges(pair));

        Position position =
                positionCommandPort.getOrCreate(order.getWalletId(), pair.tradedCoinId());
        ExecutedFill executedFill =
                ExecutedFill.of(order.getSide(), order.getFilledPrice(), order.getQuantity());
        position.applyFill(executedFill, Price.of(event.currentPrice()));
        positionCommandPort.save(position);
    }
}
