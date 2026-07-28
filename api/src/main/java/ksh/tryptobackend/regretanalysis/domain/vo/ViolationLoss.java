package ksh.tryptobackend.regretanalysis.domain.vo;

import java.time.LocalDate;

/** 그래프가 쓰는 위반 손실 한 건. 거래소가 섞이므로 금액은 원화로 환산해 담고, 실현 여부에 따라 반영 방법이 갈리므로 분해한 채로 들고 다닌다. */
public record ViolationLoss(LocalDate occurredDate, ViolationLossBreakdown krwLoss) {}
