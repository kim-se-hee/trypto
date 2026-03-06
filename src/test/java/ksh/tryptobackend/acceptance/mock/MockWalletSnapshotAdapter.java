package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.ranking.application.port.out.WalletSnapshotPort;
import ksh.tryptobackend.ranking.domain.vo.WalletSnapshot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MockWalletSnapshotAdapter implements WalletSnapshotPort {

    private final List<WalletSnapshot> wallets = new ArrayList<>();

    @Override
    public List<WalletSnapshot> findByRoundId(Long roundId) {
        return wallets.stream()
            .filter(w -> w.roundId().equals(roundId))
            .toList();
    }

    @Override
    public List<WalletSnapshot> findByRoundIds(List<Long> roundIds) {
        return wallets.stream()
            .filter(w -> roundIds.contains(w.roundId()))
            .toList();
    }

    public void addWallet(Long walletId, Long roundId, Long exchangeId, BigDecimal seedAmount) {
        wallets.add(new WalletSnapshot(walletId, roundId, exchangeId, seedAmount));
    }

    public void clear() {
        wallets.clear();
    }
}
