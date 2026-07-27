package ksh.tryptobackend.portfolio.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyAssetTotal(LocalDate snapshotDate, BigDecimal totalAssetKrw) {}
