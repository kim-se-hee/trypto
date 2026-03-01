package ksh.tryptobackend.regretanalysis.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RegretReport {

    private final Long reportId;
    private final Long userId;
    private final Long roundId;
    private final Long exchangeId;
    private final int totalViolations;
    private final BigDecimal missedProfit;
    private final BigDecimal actualProfitRate;
    private final BigDecimal ruleFollowedProfitRate;
    private final LocalDate analysisStart;
    private final LocalDate analysisEnd;
    private final LocalDateTime createdAt;
    private final List<RuleImpact> ruleImpacts;
    private final List<ViolationDetail> violationDetails;

    public static RegretReport reconstitute(Long reportId, Long userId, Long roundId, Long exchangeId,
                                            int totalViolations, BigDecimal missedProfit,
                                            BigDecimal actualProfitRate, BigDecimal ruleFollowedProfitRate,
                                            LocalDate analysisStart, LocalDate analysisEnd,
                                            LocalDateTime createdAt,
                                            List<RuleImpact> ruleImpacts,
                                            List<ViolationDetail> violationDetails) {
        return RegretReport.builder()
            .reportId(reportId)
            .userId(userId)
            .roundId(roundId)
            .exchangeId(exchangeId)
            .totalViolations(totalViolations)
            .missedProfit(missedProfit)
            .actualProfitRate(actualProfitRate)
            .ruleFollowedProfitRate(ruleFollowedProfitRate)
            .analysisStart(analysisStart)
            .analysisEnd(analysisEnd)
            .createdAt(createdAt)
            .ruleImpacts(ruleImpacts)
            .violationDetails(violationDetails)
            .build();
    }
}
