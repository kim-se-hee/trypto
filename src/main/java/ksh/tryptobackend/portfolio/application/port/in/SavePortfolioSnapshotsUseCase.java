package ksh.tryptobackend.portfolio.application.port.in;

import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotResult;

import java.util.List;

public interface SavePortfolioSnapshotsUseCase {

    void saveAll(List<SnapshotResult> results);
}
