package ksh.tryptoengine.dbwriter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptoengine.matching.OrderDetail;

public record FillCommand(
        OrderDetail order, BigDecimal executedPrice, LocalDateTime executedAt, LocalDateTime matchedAt) {}
