package ksh.tryptobackend.investmentround.adapter.out;

import ksh.tryptobackend.investmentround.adapter.out.entity.InvestmentRoundJpaEntity;
import ksh.tryptobackend.investmentround.adapter.out.repository.InvestmentRoundJpaRepository;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.dto.InvestmentRoundInfo;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InvestmentRoundQueryAdapter implements InvestmentRoundQueryPort {

    private final InvestmentRoundJpaRepository repository;

    @Override
    public Optional<InvestmentRoundInfo> findActiveRoundByUserId(Long userId) {
        return repository.findByUserIdAndStatus(userId, RoundStatus.ACTIVE)
            .map(this::toRoundInfo);
    }

    @Override
    public Optional<InvestmentRoundInfo> findRoundInfoById(Long roundId) {
        return repository.findById(roundId).map(this::toRoundInfo);
    }

    @Override
    public List<InvestmentRoundInfo> findAllActiveRounds() {
        return repository.findByStatus(RoundStatus.ACTIVE).stream()
            .map(this::toRoundInfo)
            .toList();
    }

    private InvestmentRoundInfo toRoundInfo(InvestmentRoundJpaEntity entity) {
        return new InvestmentRoundInfo(
            entity.getId(),
            entity.getUserId(),
            entity.getRoundNumber(),
            entity.getInitialSeed(),
            entity.getEmergencyFundingLimit(),
            entity.getEmergencyChargeCount(),
            entity.getStatus(),
            entity.getStartedAt(),
            entity.getEndedAt()
        );
    }
}
