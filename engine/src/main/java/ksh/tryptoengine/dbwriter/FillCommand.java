package ksh.tryptoengine.dbwriter;

import ksh.tryptoengine.matching.OrderDetail;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FillCommand(
    OrderDetail order,
    BigDecimal executedPrice,
    LocalDateTime executedAt
) {
}
