package ksh.tryptobackend.batch.snapshot;

import ksh.tryptobackend.ranking.application.port.out.ActiveRoundQueryPort;
import ksh.tryptobackend.ranking.application.port.out.WalletSnapshotPort;
import ksh.tryptobackend.ranking.application.port.out.dto.ActiveRoundInfo;
import ksh.tryptobackend.ranking.application.port.out.dto.WalletSnapshotInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@StepScope
@RequiredArgsConstructor
public class SnapshotItemReader implements ItemReader<SnapshotInput> {

    private final ActiveRoundQueryPort activeRoundQueryPort;
    private final WalletSnapshotPort walletSnapshotPort;

    private Iterator<SnapshotInput> iterator;

    @Override
    public SnapshotInput read() {
        if (iterator == null) {
            iterator = buildInputList().iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }

    private List<SnapshotInput> buildInputList() {
        List<ActiveRoundInfo> activeRounds = activeRoundQueryPort.findAllActiveRounds();
        List<Long> roundIds = activeRounds.stream().map(ActiveRoundInfo::roundId).toList();
        Map<Long, List<WalletSnapshotInfo>> walletsByRoundId = walletSnapshotPort.findByRoundIds(roundIds).stream()
            .collect(Collectors.groupingBy(WalletSnapshotInfo::roundId));

        return activeRounds.stream()
            .flatMap(round -> walletsByRoundId.getOrDefault(round.roundId(), List.of()).stream()
                .map(wallet -> toSnapshotInput(round, wallet)))
            .toList();
    }

    private SnapshotInput toSnapshotInput(ActiveRoundInfo round, WalletSnapshotInfo wallet) {
        return new SnapshotInput(
            round.roundId(), round.userId(), wallet.exchangeId(),
            wallet.walletId(), wallet.seedAmount()
        );
    }
}
