package ksh.tryptobackend.portfolio.adapter.out.persistence.repository;

import java.util.List;
import ksh.tryptobackend.portfolio.adapter.out.persistence.entity.SnapshotDetailJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapshotDetailJpaRepository extends JpaRepository<SnapshotDetailJpaEntity, Long> {

    List<SnapshotDetailJpaEntity> findBySnapshotId(Long snapshotId);
}
