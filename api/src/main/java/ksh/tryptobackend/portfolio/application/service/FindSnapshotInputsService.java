package ksh.tryptobackend.portfolio.application.service;

import java.util.List;
import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotInputsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotInputResult;
import ksh.tryptobackend.portfolio.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.portfolio.application.port.out.WalletQueryPort;
import ksh.tryptobackend.portfolio.domain.vo.ActiveRounds;
import ksh.tryptobackend.portfolio.domain.vo.WalletSnapshots;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindSnapshotInputsService implements FindSnapshotInputsUseCase {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final WalletQueryPort walletQueryPort;

    @Override
    public List<SnapshotInputResult> findAllSnapshotInputs() {
        ActiveRounds activeRounds = investmentRoundQueryPort.findActiveRounds();
        WalletSnapshots walletSnapshots = walletQueryPort.findWalletSnapshots(activeRounds.roundIds());
        return toSnapshotInputResults(activeRounds, walletSnapshots);
    }

    private List<SnapshotInputResult> toSnapshotInputResults(
            ActiveRounds activeRounds, WalletSnapshots walletSnapshots) {
        return activeRounds.values().stream()
                .flatMap(round -> walletSnapshots.findByRoundId(round.roundId()).stream()
                        .map(wallet -> new SnapshotInputResult(
                                round.roundId(),
                                round.userId(),
                                wallet.exchangeId(),
                                wallet.walletId(),
                                wallet.seedAmount())))
                .toList();
    }
}
