package ksh.tryptobackend.investmentround.adapter.out.persistence;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.adapter.out.persistence.entity.InvestmentRoundJpaEntity;
import ksh.tryptobackend.investmentround.adapter.out.persistence.repository.InvestmentRoundJpaRepository;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundCommandPort;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaInvestmentRoundCommandAdapter implements InvestmentRoundCommandPort {

    private final InvestmentRoundJpaRepository repository;

    @Override
    public InvestmentRound save(InvestmentRound round) {
        if (round.getId() == null) {
            return repository.save(InvestmentRoundJpaEntity.fromDomain(round)).toDomain();
        }
        InvestmentRoundJpaEntity entity =
                repository
                        .findById(round.getId())
                        .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_FOUND));
        entity.updateFrom(round);
        return repository.saveAndFlush(entity).toDomain();
    }

    @Override
    public boolean existsActiveRoundByUserId(Long userId) {
        return repository.existsByUserIdAndStatus(userId, RoundStatus.ACTIVE);
    }

    @Override
    public long countByUserId(Long userId) {
        return repository.countByUserId(userId);
    }
}
