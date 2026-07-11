package ksh.tryptobackend.user.adapter.out;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCommandAdapter implements UserCommandPort {

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
        try {
            return userJpaRepository
                    .saveAndFlush(UserJpaEntity.fromDomain(newUserFactory.get()))
                    .toDomain();
        } catch (DataIntegrityViolationException e) {
            return findRegistered(socialIdentity);
        }
    }

    private User findRegistered(SocialIdentity socialIdentity) {
        return findBySocialIdentity(socialIdentity)
                .orElseThrow(() -> new CustomException(ErrorCode.SOCIAL_LOGIN_FAILED));
    }

    private Optional<User> findBySocialIdentity(SocialIdentity socialIdentity) {
        return userJpaRepository
                .findByProviderAndProviderId(
                        socialIdentity.providerName(), socialIdentity.providerId())
                .map(UserJpaEntity::toDomain);
    }
}
