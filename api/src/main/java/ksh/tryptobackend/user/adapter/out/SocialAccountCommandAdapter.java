package ksh.tryptobackend.user.adapter.out;

import java.util.Locale;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.adapter.out.persistence.entity.SocialAccountJpaEntity;
import ksh.tryptobackend.user.adapter.out.persistence.repository.SocialAccountJpaRepository;
import ksh.tryptobackend.user.application.port.out.SocialAccountCommandPort;
import ksh.tryptobackend.user.domain.model.SocialAccount;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialAccountCommandAdapter implements SocialAccountCommandPort {

    private static final String PROVIDER_CONSTRAINT = "uk_social_identity_provider";

    private final SocialAccountJpaRepository socialAccountJpaRepository;

    @Override
    public SocialAccount register(SocialAccount socialAccount) {
        try {
            return socialAccountJpaRepository
                    .saveAndFlush(SocialAccountJpaEntity.fromDomain(socialAccount))
                    .toDomain();
        } catch (DataIntegrityViolationException e) {
            if (isProviderConflict(e)) {
                return getByIdentity(socialAccount.getSocialIdentity());
            }
            throw e;
        }
    }

    @Override
    public void save(SocialAccount socialAccount) {
        SocialAccountJpaEntity entity = socialAccountJpaRepository
                .findById(socialAccount.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.SOCIAL_ACCOUNT_NOT_FOUND));
        entity.updateFromDomain(socialAccount);
        socialAccountJpaRepository.saveAndFlush(entity);
    }

    private boolean isProviderConflict(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException violation) {
            String constraintName = violation.getConstraintName();
            return constraintName != null
                    && constraintName.toLowerCase(Locale.ROOT).contains(PROVIDER_CONSTRAINT);
        }
        return false;
    }

    private SocialAccount getByIdentity(SocialIdentity socialIdentity) {
        return socialAccountJpaRepository
                .findByProviderAndProviderId(socialIdentity.providerName(), socialIdentity.providerId())
                .map(SocialAccountJpaEntity::toDomain)
                .orElseThrow(() -> new CustomException(ErrorCode.SOCIAL_LOGIN_FAILED));
    }
}
