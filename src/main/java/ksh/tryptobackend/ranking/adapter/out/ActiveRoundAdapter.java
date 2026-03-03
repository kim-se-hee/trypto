package ksh.tryptobackend.ranking.adapter.out;

import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.ranking.application.port.out.ActiveRoundQueryPort;
import ksh.tryptobackend.ranking.application.port.out.dto.ActiveRoundInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("rankingActiveRoundAdapter")
@RequiredArgsConstructor
public class ActiveRoundAdapter implements ActiveRoundQueryPort {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;

    @Override
    public List<ActiveRoundInfo> findAllActiveRounds() {
        return investmentRoundQueryPort.findAllActiveRounds().stream()
            .map(info -> new ActiveRoundInfo(info.roundId(), info.userId(), info.startedAt()))
            .toList();
    }
}
