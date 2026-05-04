package ksh.tryptobackend.portfolio.application.port.out;

import java.util.List;
import ksh.tryptobackend.portfolio.domain.model.PortfolioSnapshot;

public interface PortfolioSnapshotCommandPort {

    PortfolioSnapshot save(PortfolioSnapshot snapshot);

    List<PortfolioSnapshot> saveAll(List<PortfolioSnapshot> snapshots);
}
