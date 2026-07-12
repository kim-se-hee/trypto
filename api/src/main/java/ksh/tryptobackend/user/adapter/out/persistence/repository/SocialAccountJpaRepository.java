package ksh.tryptobackend.user.adapter.out.persistence.repository;

import java.util.Optional;
import ksh.tryptobackend.user.adapter.out.persistence.entity.SocialAccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountJpaRepository extends JpaRepository<SocialAccountJpaEntity, Long> {

    Optional<SocialAccountJpaEntity> findByProviderAndProviderId(String provider, String providerId);
}
