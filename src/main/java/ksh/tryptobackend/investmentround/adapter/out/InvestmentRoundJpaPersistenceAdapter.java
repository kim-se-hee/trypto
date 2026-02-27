package ksh.tryptobackend.investmentround.adapter.out;

import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundPersistencePort;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvestmentRoundJpaPersistenceAdapter implements InvestmentRoundPersistencePort {

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
}
