package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.application.port.out.dto.WalletSnapshotInfo;

import java.util.List;

public interface WalletSnapshotPort {

    List<WalletSnapshotInfo> findByRoundId(Long roundId);

    List<WalletSnapshotInfo> findByRoundIds(List<Long> roundIds);
}
