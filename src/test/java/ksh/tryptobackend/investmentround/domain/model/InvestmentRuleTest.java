package ksh.tryptobackend.investmentround.domain.model;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.domain.vo.RuleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvestmentRuleTest {

    @Test
    @DisplayName("비율 원칙의 기준값이 0 이하이면 예외")
    void validateThreshold_rateRuleWithZero_throws() {
        assertThatThrownBy(() -> InvestmentRule.validateThreshold(RuleType.LOSS_CUT, BigDecimal.ZERO))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_RULE_THRESHOLD);
    }

    @Test
    @DisplayName("횟수 원칙의 기준값이 소수면 예외")
    void validateThreshold_countRuleWithDecimal_throws() {
        assertThatThrownBy(() -> InvestmentRule.validateThreshold(
            RuleType.AVERAGING_DOWN_LIMIT, new BigDecimal("1.5")))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_RULE_THRESHOLD);
    }
}
