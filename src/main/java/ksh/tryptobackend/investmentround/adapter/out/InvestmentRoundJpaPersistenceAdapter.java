package ksh.tryptobackend.investmentround.adapter.out;

import ksh.tryptobackend.investmentround.adapter.out.entity.InvestmentRoundJpaEntity;
import ksh.tryptobackend.investmentround.adapter.out.repository.InvestmentRoundJpaRepository;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundPersistencePort;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.dto.InvestmentRoundInfo;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InvestmentRoundJpaPersistenceAdapter implements InvestmentRoundPersistencePort, InvestmentRoundQueryPort {

    private final InvestmentRoundJpaRepository repository;

    @Override
    public boolean existsActiveRoundByUserId(Long userId) {
        return repository.existsByUserIdAndStatus(userId, RoundStatus.ACTIVE);
    }

    @Override
    public long countByUserId(Long userId) {
        return repository.countByUserId(userId);
    }

    @Override
    public InvestmentRound save(InvestmentRound round) {
        InvestmentRoundJpaEntity saved = repository.save(InvestmentRoundJpaEntity.fromDomain(round));
        return saved.toDomain();
    }

    @Override
    public Optional<InvestmentRoundInfo> findActiveRoundByUserId(Long userId) {
        return repository.findByUserIdAndStatus(userId, RoundStatus.ACTIVE)
            .map(entity -> new InvestmentRoundInfo(entity.getId(), entity.getUserId()));
    }
}
