package ksh.tryptobackend.ranking.adapter.out.acl;

import java.util.Optional;
import ksh.tryptobackend.investmentround.application.port.in.FindRoundInfoUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RoundInfoResult;
import ksh.tryptobackend.ranking.application.port.out.InvestmentRoundQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AclInvestmentRoundQueryAdapter implements InvestmentRoundQueryPort {

    private final FindRoundInfoUseCase findRoundInfoUseCase;

    @Override
    public Optional<Long> findActiveRoundId(Long userId) {
        return findRoundInfoUseCase.findActiveByUserId(userId).map(RoundInfoResult::roundId);
    }
}
