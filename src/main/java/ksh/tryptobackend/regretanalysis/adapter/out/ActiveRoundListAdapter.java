package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.dto.InvestmentRoundInfo;
import ksh.tryptobackend.regretanalysis.application.port.out.ActiveRoundListPort;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.RoundExchangeInfo;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import ksh.tryptobackend.wallet.application.port.out.dto.WalletInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component("regretActiveRoundListAdapter")
@RequiredArgsConstructor
public class ActiveRoundListAdapter implements ActiveRoundListPort {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final WalletQueryPort walletQueryPort;

    @Override
    public List<RoundExchangeInfo> findAllActiveRoundExchanges() {
        List<InvestmentRoundInfo> activeRounds = investmentRoundQueryPort.findAllActiveRounds();
        List<Long> roundIds = activeRounds.stream().map(InvestmentRoundInfo::roundId).toList();
        Map<Long, List<WalletInfo>> walletsByRoundId = walletQueryPort.findByRoundIds(roundIds).stream()
            .collect(Collectors.groupingBy(WalletInfo::roundId));

        return activeRounds.stream()
            .flatMap(round -> walletsByRoundId.getOrDefault(round.roundId(), List.of()).stream()
                .map(wallet -> new RoundExchangeInfo(
                    round.roundId(), round.userId(), wallet.exchangeId(),
                    wallet.walletId(), round.startedAt()
                )))
            .toList();
    }
}
