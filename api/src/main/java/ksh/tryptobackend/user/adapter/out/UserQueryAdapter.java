package ksh.tryptobackend.user.adapter.out;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import ksh.tryptobackend.user.adapter.out.persistence.entity.UserJpaEntity;
import ksh.tryptobackend.user.adapter.out.persistence.repository.UserJpaRepository;
import ksh.tryptobackend.user.application.port.out.UserQueryPort;
import ksh.tryptobackend.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserQueryAdapter implements UserQueryPort {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findById(Long userId) {
        return userJpaRepository.findById(userId).map(UserJpaEntity::toDomain);
    }

    @Override
    public List<User> findByIds(Set<Long> userIds) {
        return userJpaRepository.findAllById(userIds).stream()
                .map(UserJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<User> findLatestWithdrawnBySocialAccountId(Long socialAccountId) {
        return userJpaRepository
                .findFirstBySocialAccountIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(socialAccountId)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return userJpaRepository.existsByNickname(nickname);
    }
}
