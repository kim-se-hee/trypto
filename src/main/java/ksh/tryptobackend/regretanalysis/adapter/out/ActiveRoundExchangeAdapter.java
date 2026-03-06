package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.investmentround.application.port.in.FindActiveRoundsUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.ActiveRoundResult;
import ksh.tryptobackend.regretanalysis.application.port.out.ActiveRoundExchangePort;
import ksh.tryptobackend.regretanalysis.domain.vo.ActiveRoundExchange;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ActiveRoundExchangeAdapter implements ActiveRoundExchangePort {

    private final FindActiveRoundsUseCase findActiveRoundsUseCase;
    private final FindWalletUseCase findWalletUseCase;

    @Override
    public List<ActiveRoundExchange> findAllActiveRoundExchanges() {
        List<ActiveRoundResult> activeRounds = findActiveRoundsUseCase.findAllActiveRounds();
        List<Long> roundIds = activeRounds.stream().map(ActiveRoundResult::roundId).toList();
        Map<Long, List<WalletResult>> walletsByRoundId = findWalletUseCase.findByRoundIds(roundIds).stream()
            .collect(Collectors.groupingBy(WalletResult::roundId));

        return activeRounds.stream()
            .flatMap(round -> walletsByRoundId.getOrDefault(round.roundId(), List.of()).stream()
                .map(wallet -> new ActiveRoundExchange(
                    round.roundId(), round.userId(), wallet.exchangeId(),
                    wallet.walletId(), round.startedAt()
                )))
            .toList();
    }
}
