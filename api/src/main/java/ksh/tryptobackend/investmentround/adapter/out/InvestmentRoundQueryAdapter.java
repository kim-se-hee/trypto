package ksh.tryptobackend.investmentround.adapter.out;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.investmentround.adapter.out.entity.InvestmentRoundJpaEntity;
import ksh.tryptobackend.investmentround.adapter.out.repository.InvestmentRoundJpaRepository;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.investmentround.domain.vo.RoundOverview;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvestmentRoundQueryAdapter implements InvestmentRoundQueryPort {

    private final InvestmentRoundJpaRepository repository;

    @Override
    public Optional<RoundOverview> findActiveRoundByUserId(Long userId) {
        return repository
                .findByUserIdAndStatus(userId, RoundStatus.ACTIVE)
                .map(this::toRoundOverview);
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
