package ksh.tryptobackend.ranking.adapter.out;

import ksh.tryptobackend.investmentround.application.port.in.FindRoundInfoUseCase;
import ksh.tryptobackend.ranking.application.port.out.ActiveRoundPort;
import ksh.tryptobackend.ranking.domain.vo.ActiveRound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ActiveRoundAdapter implements ActiveRoundPort {

    private final FindRoundInfoUseCase findRoundInfoUseCase;

    @Override
    public Optional<ActiveRound> findActiveRoundByUserId(Long userId) {
        return findRoundInfoUseCase.findActiveByUserId(userId)
            .map(result -> new ActiveRound(result.roundId(), result.userId()));
    }
}
