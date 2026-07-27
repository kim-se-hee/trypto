package ksh.tryptobackend.portfolio.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyAssetTotalResult(LocalDate snapshotDate, BigDecimal totalAssetKrw) {}
