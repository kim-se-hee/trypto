package ksh.tryptobackend.marketdata.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyClosePrice(LocalDate date, BigDecimal closePrice) {}
