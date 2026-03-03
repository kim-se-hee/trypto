package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.ActiveRoundListPort;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.RoundExchangeInfo;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("regretActiveRoundListAdapter")
@RequiredArgsConstructor
public class ActiveRoundListAdapter implements ActiveRoundListPort {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final WalletQueryPort walletQueryPort;

    @Override
    public List<RoundExchangeInfo> findAllActiveRoundExchanges() {
        return investmentRoundQueryPort.findAllActiveRounds().stream()
            .flatMap(round -> walletQueryPort.findByRoundId(round.roundId()).stream()
                .map(wallet -> new RoundExchangeInfo(
                    round.roundId(), round.userId(), wallet.exchangeId(),
                    wallet.walletId(), round.startedAt()
                )))
            .toList();
    }
}
