package ksh.tryptobackend.trading.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import ksh.tryptobackend.trading.application.port.in.PlaceOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.application.port.out.MarketQueryPort;
import ksh.tryptobackend.trading.application.port.out.OrderCommandPort;
import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.service.WalletBalanceService;
import ksh.tryptobackend.trading.domain.vo.BalanceChange;
import ksh.tryptobackend.trading.domain.vo.MarketInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceOrderService implements PlaceOrderUseCase {

    private final OrderQueryPort orderQueryPort;
    private final OrderCommandPort orderCommandPort;
    private final MarketQueryPort marketQueryPort;
    private final WalletBalanceService walletBalanceService;
    private final Clock clock;

    @Override
    @Transactional
    public Order placeOrder(PlaceOrderCommand cmd) {
        Order duplicate = orderQueryPort.findByIdempotencyKey(cmd.idempotencyKey()).orElse(null);
        if (duplicate != null) return duplicate;

        MarketInfo marketInfo = marketQueryPort.findByExchangeCoinId(cmd.exchangeCoinId());
        LocalDateTime now = LocalDateTime.now(clock);
        Order order = Order.create(cmd, marketInfo, now);

        BalanceChange reservation = order.planReservation(marketInfo.tradingPair());
        walletBalanceService.apply(order.getWalletId(), reservation);

        return orderCommandPort.save(order);
    }
}
