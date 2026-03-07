package ksh.tryptobackend.portfolio.adapter.out;

import ksh.tryptobackend.portfolio.application.port.out.WalletSnapshotQueryPort;
import ksh.tryptobackend.portfolio.domain.vo.WalletSnapshot;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WalletSnapshotQueryAdapter implements WalletSnapshotQueryPort {

    private final FindWalletUseCase findWalletUseCase;

    @Override
    public List<WalletSnapshot> findByRoundId(Long roundId) {
        return findWalletUseCase.findByRoundId(roundId).stream()
            .map(this::toWalletSnapshot)
            .toList();
    }

    @Override
    public List<WalletSnapshot> findByRoundIds(List<Long> roundIds) {
        return findWalletUseCase.findByRoundIds(roundIds).stream()
            .map(this::toWalletSnapshot)
            .toList();
    }

    private WalletSnapshot toWalletSnapshot(WalletResult result) {
        return new WalletSnapshot(result.walletId(), result.roundId(), result.exchangeId(), result.seedAmount());
    }
}
