package ksh.tryptobackend.regretanalysis.application.port.out.dto;

import ksh.tryptobackend.regretanalysis.domain.vo.TradeSide;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeRecord(
    Long orderId,
    Long walletId,
    Long exchangeCoinId,
    TradeSide side,
    BigDecimal amount,
    BigDecimal quantity,
    BigDecimal filledPrice,
    LocalDateTime filledAt
) {
}
