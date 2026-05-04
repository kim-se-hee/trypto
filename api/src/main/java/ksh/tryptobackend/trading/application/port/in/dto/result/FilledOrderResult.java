package ksh.tryptobackend.trading.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.trading.domain.vo.FilledOrder;

public record FilledOrderResult(
        Long orderId,
        Long walletId,
        Long exchangeCoinId,
        String side,
        BigDecimal amount,
        BigDecimal quantity,
        BigDecimal filledPrice,
        LocalDateTime filledAt) {

    public static FilledOrderResult from(FilledOrder info) {
        return new FilledOrderResult(
                info.orderId(),
                info.walletId(),
                info.exchangeCoinId(),
                info.side().name(),
                info.amount(),
                info.quantity(),
                info.filledPrice(),
                info.filledAt());
    }
}
