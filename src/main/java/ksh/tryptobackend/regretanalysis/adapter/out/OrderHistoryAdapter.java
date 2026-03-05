package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.trading.application.port.out.OrderQueryPort;
import ksh.tryptobackend.trading.application.port.out.dto.OrderInfo;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.regretanalysis.application.port.out.OrderHistoryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.TradeRecord;
import ksh.tryptobackend.regretanalysis.domain.vo.TradeSide;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderHistoryAdapter implements OrderHistoryPort {

    private final OrderQueryPort orderQueryPort;

    @Override
    public List<TradeRecord> findByOrderIds(List<Long> orderIds) {
        return orderQueryPort.findFilledByOrderIds(orderIds).stream()
            .map(this::toTradeRecord)
            .toList();
    }

    @Override
    public List<TradeRecord> findSellOrdersAfter(Long walletId, Long exchangeCoinId, LocalDateTime after) {
        return orderQueryPort.findFilledSellOrders(walletId, exchangeCoinId, after).stream()
            .map(this::toTradeRecord)
            .toList();
    }

    private TradeRecord toTradeRecord(OrderInfo info) {
        return new TradeRecord(
            info.orderId(), info.walletId(), info.exchangeCoinId(),
            toTradeSide(info.side()),
            info.amount(), info.quantity(), info.filledPrice(), info.filledAt()
        );
    }

    private TradeSide toTradeSide(Side side) {
        return switch (side) {
            case BUY -> TradeSide.BUY;
            case SELL -> TradeSide.SELL;
        };
    }
}
