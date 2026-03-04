package ksh.tryptobackend.ranking.application.port.in.dto.result;

import ksh.tryptobackend.ranking.domain.model.PortfolioSnapshot;
import ksh.tryptobackend.ranking.domain.model.SnapshotDetail;

import java.util.List;

public record SnapshotResult(PortfolioSnapshot snapshot, List<SnapshotDetail> details) {
}
