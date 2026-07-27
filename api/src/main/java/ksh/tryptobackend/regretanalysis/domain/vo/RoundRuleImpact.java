package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;

/**
 * 라운드 하나로 합친 원칙별 손실. 거래소마다 남은 규칙별 손실 행을 원칙 기준으로 더한 결과라 행 하나에 대응하지 않는다. 그래서 식별자를 두지 않고 원칙으로만 구분한다.
 */
public record RoundRuleImpact(AnalysisRule rule, int violationCount, BigDecimal totalLossAmountKrw) {

    public static RoundRuleImpact notViolated(AnalysisRule rule) {
        return new RoundRuleImpact(rule, 0, BigDecimal.ZERO);
    }
}
