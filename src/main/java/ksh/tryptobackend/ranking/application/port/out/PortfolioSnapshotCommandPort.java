package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.domain.model.PortfolioSnapshot;

import java.util.List;

public interface PortfolioSnapshotCommandPort {

    PortfolioSnapshot save(PortfolioSnapshot snapshot);

    List<PortfolioSnapshot> saveAll(List<PortfolioSnapshot> snapshots);
}
