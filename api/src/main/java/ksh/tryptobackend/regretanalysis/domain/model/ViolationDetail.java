package ksh.tryptobackend.regretanalysis.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import ksh.tryptobackend.regretanalysis.domain.vo.RealizedLoss;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLossBreakdown;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ViolationDetail {

    private final Long violationDetailId;
    private final Long reportId;
    private final Long orderId;
    private final Long ruleId;
    private final Long coinId;
    private final BigDecimal lossAmount;
    private final List<RealizedLoss> realizedLosses;
    private final LocalDateTime occurredAt;

    public static ViolationDetail create(
            Long orderId, Long ruleId, Long coinId, ViolationLossBreakdown loss, LocalDateTime occurredAt) {
        return ViolationDetail.builder()
                .orderId(orderId)
                .ruleId(ruleId)
                .coinId(coinId)
                .lossAmount(loss.totalAmount())
                .realizedLosses(loss.realizedLosses())
                .occurredAt(occurredAt)
                .build();
    }

    public boolean isOrderViolation() {
        return orderId != null;
    }

    public boolean isMonitoringViolation() {
        return orderId == null;
    }

    public LocalDate getOccurredDate() {
        return occurredAt.toLocalDate();
    }

    /** 아직 매도되지 않아 금액이 현재가를 따라 움직이는 몫. 실현분과 달리 준수 시 자산에 금액 그대로 더한다. */
    public BigDecimal getUnrealizedLossAmount() {
        return realizedLosses.stream().map(RealizedLoss::amount).reduce(lossAmount, BigDecimal::subtract);
    }

    public static ViolationDetail reconstitute(
            Long violationDetailId,
            Long reportId,
            Long orderId,
            Long ruleId,
            Long coinId,
            BigDecimal lossAmount,
            List<RealizedLoss> realizedLosses,
            LocalDateTime occurredAt) {
        return ViolationDetail.builder()
                .violationDetailId(violationDetailId)
                .reportId(reportId)
                .orderId(orderId)
                .ruleId(ruleId)
                .coinId(coinId)
                .lossAmount(lossAmount)
                .realizedLosses(realizedLosses)
                .occurredAt(occurredAt)
                .build();
    }
}
