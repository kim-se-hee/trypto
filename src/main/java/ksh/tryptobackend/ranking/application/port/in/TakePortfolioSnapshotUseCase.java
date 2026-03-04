package ksh.tryptobackend.ranking.application.port.in;

import ksh.tryptobackend.ranking.application.port.in.dto.command.TakeSnapshotCommand;
import ksh.tryptobackend.ranking.application.port.in.dto.result.SnapshotResult;

public interface TakePortfolioSnapshotUseCase {

    SnapshotResult takeSnapshot(TakeSnapshotCommand command);
}
