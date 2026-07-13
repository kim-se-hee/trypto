package ksh.tryptobackend.user.adapter.out.persistence.repository;

import ksh.tryptobackend.user.adapter.out.persistence.entity.FeedbackJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackJpaRepository extends JpaRepository<FeedbackJpaEntity, Long> {}
