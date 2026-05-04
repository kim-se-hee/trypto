package ksh.tryptobackend.portfolio.application.port.in;

import java.util.List;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotResult;

public interface SavePortfolioSnapshotsUseCase {

    void saveAll(List<SnapshotResult> results);
}
