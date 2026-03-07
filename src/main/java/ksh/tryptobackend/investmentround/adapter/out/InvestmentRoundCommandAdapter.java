package ksh.tryptobackend.investmentround.adapter.out;

import ksh.tryptobackend.investmentround.adapter.out.entity.InvestmentRoundJpaEntity;
import ksh.tryptobackend.investmentround.adapter.out.repository.InvestmentRoundJpaRepository;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundCommandPort;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InvestmentRoundCommandAdapter implements InvestmentRoundCommandPort {

    private final InvestmentRoundJpaRepository repository;

    @Override
    public InvestmentRound save(InvestmentRound round) {
        InvestmentRoundJpaEntity saved = repository.save(InvestmentRoundJpaEntity.fromDomain(round));
        return saved.toDomain();
    }

    @Override
    public boolean existsActiveRoundByUserId(Long userId) {
        return repository.existsByUserIdAndStatus(userId, RoundStatus.ACTIVE);
    }

    @Override
    public long countByUserId(Long userId) {
        return repository.countByUserId(userId);
    }

    @Override
    public Optional<InvestmentRound> findById(Long roundId) {
        return repository.findById(roundId).map(InvestmentRoundJpaEntity::toDomain);
    }
}
