package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.domain.model.PortfolioSnapshot;
import ksh.tryptobackend.ranking.domain.model.SnapshotDetail;

import java.util.List;
import java.util.Map;

public interface SnapshotPersistencePort {

    PortfolioSnapshot save(PortfolioSnapshot snapshot);

    void saveDetails(Long snapshotId, List<SnapshotDetail> details);

    List<PortfolioSnapshot> saveAll(List<PortfolioSnapshot> snapshots);

    void saveAllDetails(Map<Long, List<SnapshotDetail>> snapshotDetailsMap);
}
