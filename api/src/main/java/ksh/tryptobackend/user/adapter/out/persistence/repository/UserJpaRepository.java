package ksh.tryptobackend.user.adapter.out.persistence.repository;

import java.util.Optional;
import ksh.tryptobackend.user.adapter.out.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    boolean existsByNickname(String nickname);

    Optional<UserJpaEntity> findFirstBySocialAccountIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(Long socialAccountId);
}
