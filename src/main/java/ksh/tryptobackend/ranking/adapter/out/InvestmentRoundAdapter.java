package ksh.tryptobackend.ranking.adapter.out;

import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.ranking.application.port.out.InvestmentRoundPort;
import ksh.tryptobackend.ranking.application.port.out.dto.RoundInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InvestmentRoundAdapter implements InvestmentRoundPort {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;

    @Override
    public Optional<RoundInfo> findActiveRoundByUserId(Long userId) {
        return investmentRoundQueryPort.findActiveRoundByUserId(userId)
            .map(info -> new RoundInfo(info.roundId(), info.userId()));
    }
}
