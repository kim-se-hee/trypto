package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.dto.InvestmentRoundInfo;
import ksh.tryptobackend.regretanalysis.application.port.out.InvestmentRoundPort;
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
            info.status(), info.startedAt(), info.endedAt()
        );
    }
}
