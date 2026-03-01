package ksh.tryptobackend.regretanalysis.application.port.out.dto;

import ksh.tryptobackend.trading.domain.vo.Side;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderInfo(
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
