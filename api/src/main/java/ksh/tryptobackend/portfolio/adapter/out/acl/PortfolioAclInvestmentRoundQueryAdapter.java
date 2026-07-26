package ksh.tryptobackend.portfolio.adapter.out.acl;

import java.util.List;
import ksh.tryptobackend.investmentround.application.port.in.FindActiveRoundsUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.ActiveRoundResult;
import ksh.tryptobackend.portfolio.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.portfolio.domain.vo.ActiveRound;
import ksh.tryptobackend.portfolio.domain.vo.ActiveRounds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortfolioAclInvestmentRoundQueryAdapter implements InvestmentRoundQueryPort {

    private final FindActiveRoundsUseCase findActiveRoundsUseCase;

    @Override
    public ActiveRounds findActiveRounds() {
        List<ActiveRound> rounds = findActiveRoundsUseCase.findAllActiveRounds().stream()
                .map(this::toActiveRound)
                .toList();
        return new ActiveRounds(rounds);
    }

    private ActiveRound toActiveRound(ActiveRoundResult result) {
        return new ActiveRound(result.roundId(), result.userId(), result.startedAt());
    }
}
