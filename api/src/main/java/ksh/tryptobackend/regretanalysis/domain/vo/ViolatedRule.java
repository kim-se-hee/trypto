package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import ksh.tryptobackend.common.domain.vo.RuleType;

/** 위반 거래가 어긴 원칙 하나와 그 원칙 몫의 위반 손실. 원칙 골라 보기가 곡선을 다시 그리려면 원화 금액이 실현·미실현으로 나뉘어 있어야 한다. */
public record ViolatedRule(RuleType ruleType, BigDecimal lossAmount, ViolationLossBreakdown krwLoss) {

    public BigDecimal lossAmountKrw() {
        return krwLoss.totalAmount();
    }
}
