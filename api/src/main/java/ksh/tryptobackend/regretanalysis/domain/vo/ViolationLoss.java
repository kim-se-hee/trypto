package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ViolationLoss(LocalDate occurredDate, BigDecimal amountKrw) {}
