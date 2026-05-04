package ksh.tryptobackend.portfolio.application.port.in;

import java.util.List;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotDetailResult;

public interface FindSnapshotDetailsUseCase {

    List<SnapshotDetailResult> findLatestSnapshotDetails(Long userId, Long roundId);
}
