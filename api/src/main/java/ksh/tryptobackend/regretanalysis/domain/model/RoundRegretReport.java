package ksh.tryptobackend.regretanalysis.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ksh.tryptobackend.regretanalysis.domain.vo.RoundRuleImpact;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationRow;
import lombok.Builder;
import lombok.Getter;

/** 거래소별로 생성된 리포트를 라운드 하나로 합친 복기 리포트. 요약과 규칙별 손실의 금액은 모두 원화다. */
@Getter
@Builder
public class RoundRegretReport {

    private final Long roundId;
    private final int totalViolations;
    private final BigDecimal totalViolationLoss;
    private final BigDecimal actualAsset;
    private final BigDecimal ruleFollowedAsset;
    private final LocalDate analysisStart;
    private final LocalDate analysisEnd;
    private final List<RoundRuleImpact> ruleImpacts;
    private final List<ViolationRow> violationRows;

    public static RoundRegretReport of(
            Long roundId,
            int totalViolations,
            BigDecimal totalViolationLoss,
            BigDecimal actualAsset,
            LocalDate analysisStart,
            LocalDate analysisEnd,
            List<RoundRuleImpact> ruleImpacts,
            List<ViolationRow> violationRows) {
        return RoundRegretReport.builder()
                .roundId(roundId)
                .totalViolations(totalViolations)
                .totalViolationLoss(totalViolationLoss)
                .actualAsset(actualAsset)
                .ruleFollowedAsset(actualAsset.add(totalViolationLoss))
                .analysisStart(analysisStart)
                .analysisEnd(analysisEnd)
                .ruleImpacts(ruleImpacts)
                .violationRows(violationRows)
                .build();
    }
}
