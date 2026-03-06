package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BtcDailyPrice(
    LocalDate date,
    BigDecimal closePrice
) {
}
