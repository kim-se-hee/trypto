package ksh.tryptobackend.trading.application.port.in.dto.result;

import java.math.BigDecimal;

public record HoldingInfoResult(Long coinId, BigDecimal avgBuyPrice, BigDecimal totalQuantity) {
}
