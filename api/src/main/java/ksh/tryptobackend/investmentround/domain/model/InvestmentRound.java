package ksh.tryptobackend.investmentround.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import lombok.Getter;

@Getter
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
    private int emergencyChargeCount;
    private RoundStatus status;
    private final LocalDateTime startedAt;
    private LocalDateTime endedAt;

    private final List<RuleSetting> rules;
    private final List<EmergencyFunding> fundings;

    private InvestmentRound(
            Long roundId,
            Long version,
            Long userId,
            long roundNumber,
            BigDecimal initialSeed,
            BigDecimal emergencyFundingLimit,
            int emergencyChargeCount,
            RoundStatus status,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            List<RuleSetting> rules,
            List<EmergencyFunding> fundings) {
        this.roundId = roundId;
        this.version = version;
        this.userId = userId;
        this.roundNumber = roundNumber;
        this.initialSeed = initialSeed;
        this.emergencyFundingLimit = emergencyFundingLimit;
        this.emergencyChargeCount = emergencyChargeCount;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.rules = rules != null ? new ArrayList<>(rules) : new ArrayList<>();
        this.fundings = fundings != null ? new ArrayList<>(fundings) : new ArrayList<>();
    }

    public static InvestmentRound start(
            Long userId,
            long previousRoundCount,
            BigDecimal initialSeed,
            BigDecimal emergencyFundingLimit,
            LocalDateTime startedAt) {
        validateEmergencyFundingLimit(emergencyFundingLimit);
        return new InvestmentRound(
                null,
                null,
                userId,
                previousRoundCount + 1,
                initialSeed,
                emergencyFundingLimit,
                DEFAULT_EMERGENCY_CHARGE_COUNT,
                RoundStatus.ACTIVE,
                startedAt,
                null,
                null,
                null);
    }

    public static InvestmentRound reconstitute(
            Long roundId,
            Long version,
            Long userId,
            long roundNumber,
            BigDecimal initialSeed,
            BigDecimal emergencyFundingLimit,
            int emergencyChargeCount,
            RoundStatus status,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            List<RuleSetting> rules,
            List<EmergencyFunding> fundings) {
        return new InvestmentRound(
                roundId,
                version,
                userId,
                roundNumber,
                initialSeed,
                emergencyFundingLimit,
                emergencyChargeCount,
                status,
                startedAt,
                endedAt,
                rules,
                fundings);
    }

    public void end(LocalDateTime endedAt) {
        if (status == RoundStatus.ENDED) {
            return;
        }
        if (status != RoundStatus.ACTIVE) {
            throw new CustomException(ErrorCode.ROUND_NOT_ACTIVE);
        }
        this.status = RoundStatus.ENDED;
        this.endedAt = endedAt;
    }

    public void chargeEmergencyFunding(BigDecimal amount) {
        validateChargeEmergencyFunding(amount);
        this.emergencyChargeCount = emergencyChargeCount - 1;
    }

    public void addRules(List<RuleSetting> newRules) {
        this.rules.addAll(newRules);
    }

    public void addFunding(EmergencyFunding funding) {
        this.fundings.add(funding);
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
