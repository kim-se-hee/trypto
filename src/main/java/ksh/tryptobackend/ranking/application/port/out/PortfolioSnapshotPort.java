package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.application.port.out.dto.SnapshotDetailProjection;

import java.util.List;

public interface PortfolioSnapshotPort {

    List<SnapshotDetailProjection> findLatestSnapshotDetails(Long userId, Long roundId);
}
