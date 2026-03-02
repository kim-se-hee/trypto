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
    private final Long version;
    private final Long userId;
    private final long roundNumber;
    private final BigDecimal initialSeed;
    private final BigDecimal emergencyFundingLimit;
    private final int emergencyChargeCount;
    private final RoundStatus status;
    private final LocalDateTime startedAt;
    private final LocalDateTime endedAt;
    private final Long version;

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

    public InvestmentRound end(LocalDateTime endedAt) {
        if (status == RoundStatus.ENDED) {
            return this;
        }
        if (status != RoundStatus.ACTIVE) {
            throw new CustomException(ErrorCode.ROUND_NOT_ACTIVE);
        }

        return InvestmentRound.builder()
            .roundId(roundId)
            .version(version)
            .userId(userId)
            .roundNumber(roundNumber)
            .initialSeed(initialSeed)
            .emergencyFundingLimit(emergencyFundingLimit)
            .emergencyChargeCount(emergencyChargeCount)
            .status(RoundStatus.ENDED)
            .startedAt(startedAt)
            .endedAt(endedAt)
            .version(version)
            .build();
    }

    public InvestmentRound chargeEmergencyFunding(BigDecimal amount) {
        validateChargeEmergencyFunding(amount);

        return InvestmentRound.builder()
            .roundId(roundId)
            .version(version)
            .userId(userId)
            .roundNumber(roundNumber)
            .initialSeed(initialSeed)
            .emergencyFundingLimit(emergencyFundingLimit)
            .emergencyChargeCount(emergencyChargeCount - 1)
            .status(status)
            .startedAt(startedAt)
            .endedAt(endedAt)
            .version(version)
            .build();
    }

    public boolean isEnded() {
        return status == RoundStatus.ENDED;
    }

    public void validateOwnedBy(Long requesterUserId) {
        if (!userId.equals(requesterUserId)) {
            throw new CustomException(ErrorCode.ROUND_ACCESS_DENIED);
        }
    }

    private static void validateEmergencyFundingLimit(BigDecimal emergencyFundingLimit) {
        if (emergencyFundingLimit.compareTo(ZERO) < 0
            || emergencyFundingLimit.compareTo(MAX_EMERGENCY_FUNDING_LIMIT) > 0) {
            throw new CustomException(ErrorCode.INVALID_EMERGENCY_FUNDING_LIMIT);
        }
    }

    private void validateChargeEmergencyFunding(BigDecimal amount) {
        if (status != RoundStatus.ACTIVE) {
            throw new CustomException(ErrorCode.ROUND_NOT_ACTIVE);
        }
        if (emergencyFundingLimit.compareTo(ZERO) == 0) {
            throw new CustomException(ErrorCode.EMERGENCY_FUNDING_DISABLED);
        }
        if (emergencyChargeCount <= 0) {
            throw new CustomException(ErrorCode.EMERGENCY_FUNDING_CHANCE_EXHAUSTED);
        }
        if (amount.compareTo(ZERO) <= 0 || amount.compareTo(emergencyFundingLimit) > 0) {
            throw new CustomException(ErrorCode.INVALID_EMERGENCY_FUNDING_AMOUNT);
        }
    }
}
