package ksh.tryptobackend.ranking.adapter.out;

import ksh.tryptobackend.ranking.application.port.out.WalletSnapshotPort;
import ksh.tryptobackend.ranking.application.port.out.dto.WalletSnapshotInfo;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("rankingWalletSnapshotAdapter")
@RequiredArgsConstructor
public class WalletSnapshotAdapter implements WalletSnapshotPort {

    private final WalletQueryPort walletQueryPort;

    @Override
    public List<WalletSnapshotInfo> findByRoundId(Long roundId) {
        return walletQueryPort.findByRoundId(roundId).stream()
            .map(this::toWalletSnapshotInfo)
            .toList();
    }

    @Override
    public List<WalletSnapshotInfo> findByRoundIds(List<Long> roundIds) {
        return walletQueryPort.findByRoundIds(roundIds).stream()
            .map(this::toWalletSnapshotInfo)
            .toList();
    }

    private WalletSnapshotInfo toWalletSnapshotInfo(ksh.tryptobackend.wallet.application.port.out.dto.WalletInfo info) {
        return new WalletSnapshotInfo(info.walletId(), info.roundId(), info.exchangeId(), info.seedAmount());
    }
}
