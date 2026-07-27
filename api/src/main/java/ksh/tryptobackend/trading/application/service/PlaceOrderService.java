package ksh.tryptobackend.trading.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import ksh.tryptobackend.common.idempotency.IdempotencyKeyCommandPort;
import ksh.tryptobackend.common.idempotency.IdempotencyResourceType;
import ksh.tryptobackend.trading.application.port.in.PlaceOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.application.port.out.MarketQueryPort;
import ksh.tryptobackend.trading.application.port.out.OrderCommandPort;
import ksh.tryptobackend.trading.application.port.out.PositionCommandPort;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.model.Position;
import ksh.tryptobackend.trading.domain.service.BalanceChangeApplier;
import ksh.tryptobackend.trading.domain.service.WalletOwnershipVerifier;
import ksh.tryptobackend.trading.domain.vo.BalanceChange;
import ksh.tryptobackend.trading.domain.vo.HoldingSnapshot;
import ksh.tryptobackend.trading.domain.vo.MarketInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceOrderService implements PlaceOrderUseCase {

    private final IdempotencyKeyCommandPort idempotencyKeyCommandPort;
    private final OrderCommandPort orderCommandPort;
    private final MarketQueryPort marketQueryPort;
    private final PositionCommandPort positionCommandPort;
    private final BalanceChangeApplier balanceChangeApplier;
    private final WalletOwnershipVerifier walletOwnershipVerifier;
    private final Clock clock;

    @Override
    @Transactional
    public Order placeOrder(PlaceOrderCommand cmd) {
        walletOwnershipVerifier.verify(cmd.walletId(), cmd.requesterId());

        LocalDateTime now = LocalDateTime.now(clock);
        idempotencyKeyCommandPort.preempt(cmd.idempotencyKey(), IdempotencyResourceType.PLACE_ORDER, now);

        MarketInfo marketInfo = marketQueryPort.findByExchangeCoinId(cmd.exchangeCoinId());

        // 체결 정산은 커밋 직전에 실행되므로 여기서 읽으면 시장가·지정가 모두 체결 전 값이다
        Position position = positionCommandPort.getOrCreate(
                cmd.walletId(), marketInfo.tradingPair().tradedCoinId());
        HoldingSnapshot holdingSnapshot =
                new HoldingSnapshot(position.isAtLoss(marketInfo.currentPrice()), position.getAveragingDownCount());

        Order order = Order.create(cmd, marketInfo, holdingSnapshot, now);

        BalanceChange reservation = order.planReservation(marketInfo.tradingPair());
        balanceChangeApplier.apply(order.getWalletId(), reservation);

        Order saved = orderCommandPort.save(order);
        idempotencyKeyCommandPort.linkResource(cmd.idempotencyKey(), saved.getId());
        return saved;
    }
}
