package ksh.tryptobackend.portfolio.adapter.out.repository;

import ksh.tryptobackend.portfolio.adapter.out.entity.SnapshotDetailJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SnapshotDetailJpaRepository extends JpaRepository<SnapshotDetailJpaEntity, Long> {

    List<SnapshotDetailJpaEntity> findBySnapshotId(Long snapshotId);
}
