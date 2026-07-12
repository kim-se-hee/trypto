package ksh.tryptobackend.user.adapter.out;

import java.util.Optional;
import ksh.tryptobackend.user.adapter.out.persistence.entity.SocialAccountJpaEntity;
import ksh.tryptobackend.user.adapter.out.persistence.repository.SocialAccountJpaRepository;
import ksh.tryptobackend.user.application.port.out.SocialAccountQueryPort;
import ksh.tryptobackend.user.domain.model.SocialAccount;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialAccountQueryAdapter implements SocialAccountQueryPort {

    private final SocialAccountJpaRepository socialAccountJpaRepository;

    @Override
    public Optional<SocialAccount> findByIdentity(SocialIdentity socialIdentity) {
        return socialAccountJpaRepository
                .findByProviderAndProviderId(socialIdentity.providerName(), socialIdentity.providerId())
                .map(SocialAccountJpaEntity::toDomain);
    }
}
