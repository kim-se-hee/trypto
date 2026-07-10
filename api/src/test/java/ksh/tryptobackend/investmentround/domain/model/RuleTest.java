package ksh.tryptobackend.investmentround.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuleTest {

    @Test
    @DisplayName("비율 원칙의 기준값이 0 이하이면 예외")
    void create_rateRuleWithZero_throws() {
        assertThatThrownBy(
                        () -> Rule.create(RuleType.LOSS_CUT, BigDecimal.ZERO, LocalDateTime.now()))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_RULE_THRESHOLD);
    }

    @Test
    @DisplayName("횟수 원칙의 기준값이 소수면 예외")
    void create_countRuleWithDecimal_throws() {
        assertThatThrownBy(
                        () ->
                                Rule.create(
                                        RuleType.AVERAGING_DOWN_LIMIT,
                                        new BigDecimal("1.5"),
                                        LocalDateTime.now()))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_RULE_THRESHOLD);
    }
}
