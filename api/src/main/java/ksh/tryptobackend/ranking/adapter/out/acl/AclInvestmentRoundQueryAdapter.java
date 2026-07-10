package ksh.tryptobackend.ranking.adapter.out.acl;

import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.FindActiveRoundsUseCase;
import ksh.tryptobackend.investmentround.application.port.in.FindRoundInfoUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.ActiveRoundResult;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RoundInfoResult;
import ksh.tryptobackend.ranking.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.ranking.domain.vo.ActiveRound;
import ksh.tryptobackend.ranking.domain.vo.ActiveRounds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("rankingAclInvestmentRoundQueryAdapter")
@RequiredArgsConstructor
public class AclInvestmentRoundQueryAdapter implements InvestmentRoundQueryPort {

    private final FindRoundInfoUseCase findRoundInfoUseCase;
    private final FindActiveRoundsUseCase findActiveRoundsUseCase;

    @Override
    public Long getActiveRoundId(Long userId) {
        return findRoundInfoUseCase
                .findActiveByUserId(userId)
                .map(RoundInfoResult::roundId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_ACTIVE));
    }

    @Override
    public ActiveRounds findActiveRounds() {
        List<ActiveRound> rounds =
                findActiveRoundsUseCase.findAllActiveRounds().stream()
                        .map(this::toActiveRound)
                        .toList();
        return new ActiveRounds(rounds);
    }

    private ActiveRound toActiveRound(ActiveRoundResult result) {
        return new ActiveRound(result.userId(), result.roundId(), result.startedAt());
    }
}
