package ksh.tryptobackend.investmentround.domain.model;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvestmentRoundTest {

    @Test
    @DisplayName("긴급 자금 상한이 100만원을 넘으면 예외")
    void startRound_emergencyFundingExceedsLimit_throws() {
        assertThatThrownBy(() -> InvestmentRound.start(
            1L, 0L, new BigDecimal("1000000"), new BigDecimal("1000001"), LocalDateTime.now()))
            .isInstanceOf(CustomException.class)
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_EMERGENCY_FUNDING_LIMIT);
    }

    @Test
    @DisplayName("라운드 시작 시 기본값이 설정된다")
    void startRound_validInput_setsDefaults() {
        InvestmentRound round = InvestmentRound.start(
            1L, 2L, new BigDecimal("8000100"), new BigDecimal("500000"), LocalDateTime.now());

        assertThat(round.getRoundNumber()).isEqualTo(3L);
        assertThat(round.getStatus()).hasToString("ACTIVE");
        assertThat(round.getEmergencyChargeCount()).isEqualTo(3);
    }
}
