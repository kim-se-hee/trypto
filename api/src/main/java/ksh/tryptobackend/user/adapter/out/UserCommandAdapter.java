package ksh.tryptobackend.user.adapter.out;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.adapter.out.persistence.entity.UserJpaEntity;
import ksh.tryptobackend.user.adapter.out.persistence.repository.UserJpaRepository;
import ksh.tryptobackend.user.application.port.out.UserCommandPort;
import ksh.tryptobackend.user.domain.model.User;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCommandAdapter implements UserCommandPort {

    private static final String SOCIAL_IDENTITY_CONSTRAINT = "uk_user_social_identity";
    private static final String NICKNAME_CONSTRAINT = "uk_user_nickname";
    private static final int MAX_REGISTER_ATTEMPTS = 5;

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        if (user.getUserId() == null) {
            return userJpaRepository.saveAndFlush(UserJpaEntity.fromDomain(user)).toDomain();
        }
        UserJpaEntity entity =
                userJpaRepository
                        .findById(user.getUserId())
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        entity.updateFromDomain(user);
        return userJpaRepository.saveAndFlush(entity).toDomain();
    }

    @Override
    public User register(SocialIdentity socialIdentity, Supplier<User> newUserFactory) {
        for (int attempt = 0; attempt < MAX_REGISTER_ATTEMPTS; attempt++) {
            try {
                return userJpaRepository
                        .saveAndFlush(UserJpaEntity.fromDomain(newUserFactory.get()))
                        .toDomain();
            } catch (DataIntegrityViolationException e) {
                if (isSocialIdentityConflict(e)) {
                    return findRegistered(socialIdentity);
                }
                if (!isNicknameConflict(e)) {
                    throw e;
                }
            }
        }
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private User findRegistered(SocialIdentity socialIdentity) {
        return findBySocialIdentity(socialIdentity)
                .orElseThrow(() -> new CustomException(ErrorCode.SOCIAL_LOGIN_FAILED));
    }

    private boolean isSocialIdentityConflict(DataIntegrityViolationException e) {
        return violatedConstraintContains(e, SOCIAL_IDENTITY_CONSTRAINT);
    }

    private boolean isNicknameConflict(DataIntegrityViolationException e) {
        return violatedConstraintContains(e, NICKNAME_CONSTRAINT);
    }

    private boolean violatedConstraintContains(
            DataIntegrityViolationException e, String constraint) {
        if (e.getCause() instanceof ConstraintViolationException violation) {
            String constraintName = violation.getConstraintName();
            return constraintName != null
                    && constraintName.toLowerCase(Locale.ROOT).contains(constraint);
        }
        return false;
    }

    private Optional<User> findBySocialIdentity(SocialIdentity socialIdentity) {
        return userJpaRepository
                .findByProviderAndProviderId(
                        socialIdentity.providerName(), socialIdentity.providerId())
                .map(UserJpaEntity::toDomain);
    }
}
