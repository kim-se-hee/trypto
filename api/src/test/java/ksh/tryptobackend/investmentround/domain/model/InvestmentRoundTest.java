package ksh.tryptobackend.investmentround.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InvestmentRoundTest {

    @Test
    @DisplayName("Throw when emergency funding limit exceeds max")
    void startRound_emergencyFundingExceedsLimit_throws() {
        assertThatThrownBy(
                        () ->
                                InvestmentRound.start(
                                        1L,
                                        0L,
                                        new BigDecimal("1000000"),
                                        new BigDecimal("1000001"),
                                        LocalDateTime.now()))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_EMERGENCY_FUNDING_LIMIT);
    }

    @Test
    @DisplayName("Set defaults when round starts")
    void startRound_validInput_setsDefaults() {
        InvestmentRound round =
                InvestmentRound.start(
                        1L,
                        2L,
                        new BigDecimal("8000100"),
                        new BigDecimal("500000"),
                        LocalDateTime.now());

        assertThat(round.getRoundNumber()).isEqualTo(3L);
        assertThat(round.getStatus()).isEqualTo(RoundStatus.ACTIVE);
        assertThat(round.getEmergencyChargeCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("End ACTIVE round")
    void end_activeRound_changesStatusToEnded() {
        InvestmentRound round =
                InvestmentRound.start(
                        1L, 0L, new BigDecimal("1000"), new BigDecimal("100"), LocalDateTime.now());
        LocalDateTime endedAt = LocalDateTime.of(2026, 3, 1, 11, 40, 0);

        round.end(endedAt);

        assertThat(round.getStatus()).isEqualTo(RoundStatus.ENDED);
        assertThat(round.getEndedAt()).isEqualTo(endedAt);
    }

    @Test
    @DisplayName("Return same round when already ENDED")
    void end_alreadyEndedRound_returnsSameRound() {
        LocalDateTime endedAt = LocalDateTime.of(2026, 3, 1, 11, 40, 0);
        InvestmentRound round =
                InvestmentRound.reconstitute(
                        1L,
                        null,
                        1L,
                        1L,
                        new BigDecimal("1000"),
                        new BigDecimal("100"),
                        3,
                        RoundStatus.ENDED,
                        LocalDateTime.of(2026, 3, 1, 9, 0, 0),
                        endedAt,
                        List.of(),
                        List.of());

        round.end(LocalDateTime.of(2026, 3, 1, 12, 0, 0));

        assertThat(round.getStatus()).isEqualTo(RoundStatus.ENDED);
        assertThat(round.getEndedAt()).isEqualTo(endedAt);
    }

    @Test
    @DisplayName("Throw when ending BANKRUPT round")
    void end_bankruptRound_throwsRoundNotActive() {
        InvestmentRound round =
                InvestmentRound.reconstitute(
                        1L,
                        null,
                        1L,
                        1L,
                        new BigDecimal("1000"),
                        new BigDecimal("100"),
                        3,
                        RoundStatus.BANKRUPT,
                        LocalDateTime.of(2026, 3, 1, 9, 0, 0),
                        null,
                        List.of(),
                        List.of());

        assertThatThrownBy(() -> round.end(LocalDateTime.now()))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ROUND_NOT_ACTIVE);
    }

    @Test
    @DisplayName("Decrease remaining count when charging emergency funding")
    void chargeEmergencyFunding_validAmount_decreasesCount() {
        InvestmentRound round =
                InvestmentRound.start(
                        1L,
                        0L,
                        new BigDecimal("1000"),
                        new BigDecimal("500000"),
                        LocalDateTime.now());

        round.chargeEmergencyFunding(new BigDecimal("300000"));

        assertThat(round.getEmergencyChargeCount()).isEqualTo(2);
        assertThat(round.getStatus()).isEqualTo(RoundStatus.ACTIVE);
    }

    @Test
    @DisplayName("Throw disabled error when limit is zero")
    void chargeEmergencyFunding_limitZero_throwsDisabled() {
        InvestmentRound round =
                InvestmentRound.start(
                        1L, 0L, new BigDecimal("1000"), BigDecimal.ZERO, LocalDateTime.now());

        assertThatThrownBy(() -> round.chargeEmergencyFunding(new BigDecimal("1")))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.EMERGENCY_FUNDING_DISABLED);
    }

    @Test
    @DisplayName("Throw amount error when amount exceeds limit")
    void chargeEmergencyFunding_amountExceedsLimit_throwsInvalidAmount() {
        InvestmentRound round =
                InvestmentRound.start(
                        1L, 0L, new BigDecimal("1000"), new BigDecimal("100"), LocalDateTime.now());

        assertThatThrownBy(() -> round.chargeEmergencyFunding(new BigDecimal("101")))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_EMERGENCY_FUNDING_AMOUNT);
    }
}
