package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.domain.vo.WalletSnapshot;

import java.util.List;

public interface WalletSnapshotPort {

    List<WalletSnapshot> findByRoundId(Long roundId);

    List<WalletSnapshot> findByRoundIds(List<Long> roundIds);
}
