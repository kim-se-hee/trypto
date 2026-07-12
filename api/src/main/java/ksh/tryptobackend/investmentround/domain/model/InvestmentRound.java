package ksh.tryptobackend.investmentround.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.domain.vo.DetectedViolation;
import ksh.tryptobackend.investmentround.domain.vo.EmergencyFundingAllowance;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import ksh.tryptobackend.investmentround.domain.vo.RuleEvaluationInput;
import lombok.Getter;

@Getter
public class InvestmentRound {

    private final Long id;
    private final Long version;
    private final Long userId;
    private final long roundNumber;
    private final BigDecimal initialSeed;
    private final LocalDateTime startedAt;
    private final Rules rules;
    private final EmergencyFundings fundings;

    private EmergencyFundingAllowance emergencyFundingAllowance;
    private RoundStatus status;
    private LocalDateTime endedAt;

    private InvestmentRound(
            Long id,
            Long version,
            Long userId,
            long roundNumber,
            BigDecimal initialSeed,
            EmergencyFundingAllowance emergencyFundingAllowance,
            RoundStatus status,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            Rules rules,
            List<EmergencyFunding> fundings) {
        this.id = id;
        this.version = version;
        this.userId = userId;
        this.roundNumber = roundNumber;
        this.initialSeed = initialSeed;
        this.emergencyFundingAllowance = emergencyFundingAllowance;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.rules = rules != null ? rules : new Rules(List.of());
        this.fundings = new EmergencyFundings(fundings);
    }

    public static InvestmentRound start(
            Long userId,
            long previousRoundCount,
            BigDecimal initialSeed,
            BigDecimal emergencyFundingLimit,
            List<Rule> rules,
            LocalDateTime startedAt) {
        return new InvestmentRound(
                null,
                null,
                userId,
                previousRoundCount + 1,
                initialSeed,
                EmergencyFundingAllowance.initial(emergencyFundingLimit),
                RoundStatus.ACTIVE,
                startedAt,
                null,
                Rules.create(rules),
                null);
    }

    public static InvestmentRound reconstitute(
            Long id,
            Long version,
            Long userId,
            long roundNumber,
            BigDecimal initialSeed,
            BigDecimal emergencyFundingLimit,
            int emergencyChargeCount,
            RoundStatus status,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            List<Rule> rules,
            List<EmergencyFunding> fundings) {
        return new InvestmentRound(
                id,
                version,
                userId,
                roundNumber,
                initialSeed,
                EmergencyFundingAllowance.of(emergencyFundingLimit, emergencyChargeCount),
                status,
                startedAt,
                endedAt,
                new Rules(rules),
                fundings);
    }

    public void end(LocalDateTime endedAt) {
        if (isEnded()) {
            return;
        }
        if (!isActive()) {
            throw new CustomException(ErrorCode.ROUND_NOT_ACTIVE);
        }
        this.status = RoundStatus.ENDED;
        this.endedAt = endedAt;
    }

    public EmergencyFunding chargeEmergencyFunding(Long exchangeId, BigDecimal amount, LocalDateTime now) {
        if (status != RoundStatus.ACTIVE) {
            throw new CustomException(ErrorCode.ROUND_NOT_ACTIVE);
        }
        emergencyFundingAllowance.validateChargeable(amount);
        this.emergencyFundingAllowance = emergencyFundingAllowance.consume();
        EmergencyFunding funding = EmergencyFunding.create(exchangeId, amount, now);
        this.fundings.add(funding);
        return funding;
    }

    public boolean isEnded() {
        return status == RoundStatus.ENDED;
    }

    public List<DetectedViolation> detectViolations(RuleEvaluationInput context) {
        return rules.check(context);
    }

    public void validateOwnedBy(Long requesterUserId) {
        if (!userId.equals(requesterUserId)) {
            throw new CustomException(ErrorCode.ROUND_ACCESS_DENIED);
        }
    }

    public BigDecimal getEmergencyFundingLimit() {
        return emergencyFundingAllowance.limit();
    }

    public int getEmergencyChargeCount() {
        return emergencyFundingAllowance.remainingCount();
    }

    public Long latestFundingId() {
        return fundings.latestId();
    }

    private boolean isActive() {
        return status == RoundStatus.ACTIVE;
    }
}
