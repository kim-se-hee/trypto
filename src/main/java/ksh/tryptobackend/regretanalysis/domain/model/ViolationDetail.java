package ksh.tryptobackend.regretanalysis.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ViolationDetail {

    private final Long violationDetailId;
    private final Long reportId;
    private final Long orderId;
    private final Long ruleId;
    private final Long coinId;
    private final BigDecimal lossAmount;
    private final BigDecimal profitLoss;
    private final LocalDateTime occurredAt;

    public static ViolationDetail create(Long orderId, Long ruleId, Long coinId,
                                         BigDecimal lossAmount, BigDecimal profitLoss,
                                         LocalDateTime occurredAt) {
        return ViolationDetail.builder()
            .orderId(orderId)
            .ruleId(ruleId)
            .coinId(coinId)
            .lossAmount(lossAmount)
            .profitLoss(profitLoss)
            .occurredAt(occurredAt)
            .build();
    }

    public boolean isOrderViolation() {
        return orderId != null;
    }

    public boolean isMonitoringViolation() {
        return orderId == null;
    }

    public static ViolationDetail reconstitute(Long violationDetailId, Long reportId, Long orderId,
                                               Long ruleId, Long coinId, BigDecimal lossAmount,
                                               BigDecimal profitLoss, LocalDateTime occurredAt) {
        return ViolationDetail.builder()
            .violationDetailId(violationDetailId)
            .reportId(reportId)
            .orderId(orderId)
            .ruleId(ruleId)
            .coinId(coinId)
            .lossAmount(lossAmount)
            .profitLoss(profitLoss)
            .occurredAt(occurredAt)
            .build();
    }
}
