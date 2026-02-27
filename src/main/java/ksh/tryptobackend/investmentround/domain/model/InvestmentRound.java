package ksh.tryptobackend.investmentround.domain.model;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class InvestmentRound {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal MAX_EMERGENCY_FUNDING_LIMIT = new BigDecimal("1000000");
    private static final int DEFAULT_EMERGENCY_CHARGE_COUNT = 3;

    private final Long roundId;
    private final Long userId;
    private final long roundNumber;
    private final BigDecimal initialSeed;
    private final BigDecimal emergencyFundingLimit;
    private final int emergencyChargeCount;
    private final RoundStatus status;
    private final LocalDateTime startedAt;
    private final LocalDateTime endedAt;

    public static InvestmentRound start(Long userId, long previousRoundCount, BigDecimal initialSeed,
                                        BigDecimal emergencyFundingLimit, LocalDateTime startedAt) {
        validateEmergencyFundingLimit(emergencyFundingLimit);
        return InvestmentRound.builder()
            .userId(userId)
            .roundNumber(previousRoundCount + 1)
            .initialSeed(initialSeed)
            .emergencyFundingLimit(emergencyFundingLimit)
            .emergencyChargeCount(DEFAULT_EMERGENCY_CHARGE_COUNT)
            .status(RoundStatus.ACTIVE)
            .startedAt(startedAt)
            .endedAt(null)
            .build();
    }

    public static void validateEmergencyFundingLimit(BigDecimal emergencyFundingLimit) {
        if (emergencyFundingLimit.compareTo(ZERO) < 0
            || emergencyFundingLimit.compareTo(MAX_EMERGENCY_FUNDING_LIMIT) > 0) {
            throw new CustomException(ErrorCode.INVALID_EMERGENCY_FUNDING_LIMIT);
        }
    }
}
