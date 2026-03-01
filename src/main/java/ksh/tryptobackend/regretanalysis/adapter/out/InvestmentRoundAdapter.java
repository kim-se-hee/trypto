package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.dto.InvestmentRoundInfo;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import ksh.tryptobackend.regretanalysis.application.port.out.InvestmentRoundPort;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.AnalysisRoundStatus;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.RoundInfoResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("regretInvestmentRoundAdapter")
@RequiredArgsConstructor
public class InvestmentRoundAdapter implements InvestmentRoundPort {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;

    @Override
    public RoundInfoResult getRound(Long roundId) {
        InvestmentRoundInfo info = investmentRoundQueryPort.findRoundInfoById(roundId)
            .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_FOUND));

        return new RoundInfoResult(
            info.roundId(), info.userId(), info.initialSeed(),
            toAnalysisRoundStatus(info.status()), info.startedAt(), info.endedAt()
        );
    }

    private AnalysisRoundStatus toAnalysisRoundStatus(RoundStatus status) {
        return switch (status) {
            case ACTIVE -> AnalysisRoundStatus.ACTIVE;
            case BANKRUPT -> AnalysisRoundStatus.BANKRUPT;
            case ENDED -> AnalysisRoundStatus.ENDED;
        };
    }
}
