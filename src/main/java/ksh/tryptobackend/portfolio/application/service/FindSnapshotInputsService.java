package ksh.tryptobackend.portfolio.application.service;

import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotInputsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotInputResult;
import ksh.tryptobackend.portfolio.application.port.out.ActiveRoundQueryPort;
import ksh.tryptobackend.portfolio.application.port.out.WalletSnapshotQueryPort;
import ksh.tryptobackend.portfolio.domain.vo.ActiveRound;
import ksh.tryptobackend.portfolio.domain.vo.WalletSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindSnapshotInputsService implements FindSnapshotInputsUseCase {

    private final ActiveRoundQueryPort activeRoundQueryPort;
    private final WalletSnapshotQueryPort walletSnapshotQueryPort;

    @Override
    public List<SnapshotInputResult> findAllSnapshotInputs() {
        List<ActiveRound> activeRounds = activeRoundQueryPort.findAllActiveRounds();
        List<Long> roundIds = activeRounds.stream().map(ActiveRound::roundId).toList();
        Map<Long, List<WalletSnapshot>> walletsByRoundId = walletSnapshotQueryPort.findByRoundIds(roundIds).stream()
            .collect(Collectors.groupingBy(WalletSnapshot::roundId));

        return activeRounds.stream()
            .flatMap(round -> walletsByRoundId.getOrDefault(round.roundId(), List.of()).stream()
                .map(wallet -> new SnapshotInputResult(
                    round.roundId(), round.userId(),
                    wallet.exchangeId(), wallet.walletId(), wallet.seedAmount())))
            .toList();
    }
}
