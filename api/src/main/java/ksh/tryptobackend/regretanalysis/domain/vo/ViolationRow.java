package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ViolationRow(
        Long violationDetailId,
        Long orderId,
        AnalysisExchange exchange,
        Long coinId,
        List<ViolatedRule> violatedRules,
        LocalDateTime occurredAt) {

    public BigDecimal totalLossAmount() {
        return violatedRules.stream().map(ViolatedRule::lossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalLossAmountKrw() {
        return violatedRules.stream().map(ViolatedRule::lossAmountKrw).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
