package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.FindRoundInfoUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RoundInfoResult;
import ksh.tryptobackend.regretanalysis.application.port.out.AnalysisRoundPort;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRound;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRoundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalysisRoundAdapter implements AnalysisRoundPort {

    private final FindRoundInfoUseCase findRoundInfoUseCase;

    @Override
    public AnalysisRound getRound(Long roundId) {
        RoundInfoResult result = findRoundInfoUseCase.findById(roundId)
            .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_FOUND));

        return new AnalysisRound(
            result.roundId(), result.userId(), result.initialSeed(),
            AnalysisRoundStatus.valueOf(result.status()),
            result.startedAt(), result.endedAt()
        );
    }
}
