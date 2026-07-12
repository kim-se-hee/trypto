package ksh.tryptobackend.investmentround.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.adapter.out.persistence.entity.InvestmentRoundJpaEntity;
import ksh.tryptobackend.investmentround.adapter.out.persistence.repository.InvestmentRoundJpaRepository;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.vo.RoundOverview;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaInvestmentRoundQueryAdapter implements InvestmentRoundQueryPort {

    private final InvestmentRoundJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public InvestmentRound getById(Long roundId) {
        return repository
                .findById(roundId)
                .map(InvestmentRoundJpaEntity::toDomain)
                .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_FOUND));
    }

    @Override
    public InvestmentRound getByIdWithLock(Long roundId) {
        return repository
                .findWithLockById(roundId)
                .map(InvestmentRoundJpaEntity::toDomain)
                .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_FOUND));
    }

    @Override
    public Optional<RoundOverview> findActiveRoundByUserId(Long userId) {
        return repository.findByUserIdAndStatus(userId, RoundStatus.ACTIVE).map(this::toRoundOverview);
    }

    @Override
    public Optional<RoundOverview> findRoundInfoById(Long roundId) {
        return repository.findById(roundId).map(this::toRoundOverview);
    }

    @Override
    public List<RoundOverview> findAllActiveRounds() {
        return repository.findByStatus(RoundStatus.ACTIVE).stream()
                .map(this::toRoundOverview)
                .toList();
    }

    private RoundOverview toRoundOverview(InvestmentRoundJpaEntity entity) {
        return new RoundOverview(
                entity.getId(),
                entity.getUserId(),
                entity.getRoundNumber(),
                entity.getInitialSeed(),
                entity.getEmergencyFundingLimit(),
                entity.getEmergencyChargeCount(),
                entity.getStatus(),
                entity.getStartedAt(),
                entity.getEndedAt());
    }
}
