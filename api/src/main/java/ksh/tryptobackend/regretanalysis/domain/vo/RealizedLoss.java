package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 매도로 확정된 위반 손익 한 조각. 금액이 실현일에 박제되므로 이후에는 그 몫을 자산 대비 비율로 환산해 반영한다. */
public record RealizedLoss(LocalDate realizedOn, BigDecimal amount) {}
