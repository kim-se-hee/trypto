package ksh.tryptobackend.investmentround.application.service;

import ksh.tryptobackend.investmentround.application.port.in.FindRoundInfoUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RoundInfoResult;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.investmentround.domain.vo.RoundOverview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FindRoundInfoService implements FindRoundInfoUseCase {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;

    @Override
    public Optional<RoundInfoResult> findById(Long roundId) {
        return investmentRoundQueryPort.findRoundInfoById(roundId).map(this::toResult);
    }

    @Override
    public Optional<RoundInfoResult> findActiveByUserId(Long userId) {
        return investmentRoundQueryPort.findActiveRoundByUserId(userId).map(this::toResult);
    }

    private RoundInfoResult toResult(RoundOverview info) {
        return new RoundInfoResult(
            info.roundId(), info.userId(), info.roundNumber(),
            info.initialSeed(), info.emergencyFundingLimit(), info.emergencyChargeCount(),
            info.status().name(), info.startedAt(), info.endedAt()
        );
    }
}
