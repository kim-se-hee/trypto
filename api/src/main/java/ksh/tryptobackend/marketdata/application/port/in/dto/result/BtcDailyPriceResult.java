package ksh.tryptobackend.marketdata.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BtcDailyPriceResult(LocalDate date, BigDecimal closePrice) {}
