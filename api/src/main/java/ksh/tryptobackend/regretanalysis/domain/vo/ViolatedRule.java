package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import ksh.tryptobackend.common.domain.vo.RuleType;

public record ViolatedRule(RuleType ruleType, BigDecimal lossAmount, BigDecimal lossAmountKrw) {}
