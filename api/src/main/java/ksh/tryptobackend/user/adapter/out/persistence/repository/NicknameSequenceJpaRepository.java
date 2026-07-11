package ksh.tryptobackend.user.adapter.out.persistence.repository;

import ksh.tryptobackend.user.adapter.out.persistence.entity.NicknameSequenceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NicknameSequenceJpaRepository
        extends JpaRepository<NicknameSequenceJpaEntity, Long> {}
