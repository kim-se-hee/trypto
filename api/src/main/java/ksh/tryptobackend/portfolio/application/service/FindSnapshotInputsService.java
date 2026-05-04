package ksh.tryptobackend.portfolio.application.service;

import java.util.List;
import ksh.tryptobackend.investmentround.application.port.in.FindActiveRoundsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotInputsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotInputResult;
import ksh.tryptobackend.portfolio.domain.vo.ActiveRound;
import ksh.tryptobackend.portfolio.domain.vo.ActiveRounds;
import ksh.tryptobackend.portfolio.domain.vo.WalletSnapshot;
import ksh.tryptobackend.portfolio.domain.vo.WalletSnapshots;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindSnapshotInputsService implements FindSnapshotInputsUseCase {

    private final FindActiveRoundsUseCase findActiveRoundsUseCase;
    private final FindWalletUseCase findWalletUseCase;

    @Override
    public List<SnapshotInputResult> findAllSnapshotInputs() {
        ActiveRounds activeRounds = findActiveRounds();
        WalletSnapshots walletSnapshots = findWalletSnapshots(activeRounds.roundIds());
        return toSnapshotInputResults(activeRounds, walletSnapshots);
    }

    private ActiveRounds findActiveRounds() {
        List<ActiveRound> rounds =
                findActiveRoundsUseCase.findAllActiveRounds().stream()
                        .map(r -> new ActiveRound(r.roundId(), r.userId(), r.startedAt()))
                        .toList();
        return new ActiveRounds(rounds);
    }

    private WalletSnapshots findWalletSnapshots(List<Long> roundIds) {
        List<WalletSnapshot> wallets =
                findWalletUseCase.findByRoundIds(roundIds).stream()
                        .map(
                                r ->
                                        new WalletSnapshot(
                                                r.walletId(),
                                                r.roundId(),
                                                r.exchangeId(),
                                                r.seedAmount()))
                        .toList();
        return new WalletSnapshots(wallets);
    }

    private List<SnapshotInputResult> toSnapshotInputResults(
            ActiveRounds activeRounds, WalletSnapshots walletSnapshots) {
        return activeRounds.values().stream()
                .flatMap(
                        round ->
                                walletSnapshots.findByRoundId(round.roundId()).stream()
                                        .map(
                                                wallet ->
                                                        new SnapshotInputResult(
                                                                round.roundId(),
                                                                round.userId(),
                                                                wallet.exchangeId(),
                                                                wallet.walletId(),
                                                                wallet.seedAmount())))
                .toList();
    }
}
