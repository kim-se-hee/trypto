package ksh.tryptobackend.portfolio.adapter.out;

import ksh.tryptobackend.investmentround.application.port.in.FindActiveRoundsUseCase;
import ksh.tryptobackend.portfolio.application.port.out.ActiveRoundQueryPort;
import ksh.tryptobackend.portfolio.domain.vo.ActiveRound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ActiveRoundQueryAdapter implements ActiveRoundQueryPort {

    private final FindActiveRoundsUseCase findActiveRoundsUseCase;

    @Override
    public List<ActiveRound> findAllActiveRounds() {
        return findActiveRoundsUseCase.findAllActiveRounds().stream()
            .map(r -> new ActiveRound(r.roundId(), r.userId(), r.startedAt()))
            .toList();
    }
}
