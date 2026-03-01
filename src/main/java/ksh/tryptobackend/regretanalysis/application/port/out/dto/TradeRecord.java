package ksh.tryptobackend.regretanalysis.application.port.out.dto;

import ksh.tryptobackend.common.domain.vo.Side;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeRecord(
    Long orderId,
    Long walletId,
    Long exchangeCoinId,
    Side side,
    BigDecimal amount,
    BigDecimal quantity,
    BigDecimal filledPrice,
    LocalDateTime filledAt
) {
}
