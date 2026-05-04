package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import ksh.tryptobackend.common.domain.vo.RuleType;

public record AnalysisRule(Long ruleId, RuleType ruleType, BigDecimal thresholdValue) {}
