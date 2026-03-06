package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.trading.application.port.in.FindFilledOrdersUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.FilledOrderResult;
import ksh.tryptobackend.regretanalysis.application.port.out.TradeRecordPort;
import ksh.tryptobackend.regretanalysis.domain.vo.TradeRecord;
import ksh.tryptobackend.regretanalysis.domain.vo.TradeSide;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TradeRecordAdapter implements TradeRecordPort {

    private final FindFilledOrdersUseCase findFilledOrdersUseCase;

    @Override
    public List<TradeRecord> findByOrderIds(List<Long> orderIds) {
        return findFilledOrdersUseCase.findByOrderIds(orderIds).stream()
            .map(this::toTradeRecord)
            .toList();
    }

    @Override
    public List<TradeRecord> findSellOrdersAfter(Long walletId, Long exchangeCoinId, LocalDateTime after) {
        return findFilledOrdersUseCase.findSellOrders(walletId, exchangeCoinId, after).stream()
            .map(this::toTradeRecord)
            .toList();
    }

    private TradeRecord toTradeRecord(FilledOrderResult result) {
        return new TradeRecord(
            result.orderId(), result.walletId(), result.exchangeCoinId(),
            TradeSide.valueOf(result.side()),
            result.amount(), result.quantity(), result.filledPrice(), result.filledAt()
        );
    }
}
