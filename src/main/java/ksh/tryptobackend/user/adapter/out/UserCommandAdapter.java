package ksh.tryptobackend.user.adapter.out;

import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.adapter.out.entity.QUserJpaEntity;
import ksh.tryptobackend.user.adapter.out.entity.UserJpaEntity;
import ksh.tryptobackend.user.adapter.out.repository.UserJpaRepository;
import ksh.tryptobackend.user.application.port.out.UserCommandPort;
import ksh.tryptobackend.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCommandAdapter implements UserCommandPort {

    private final UserJpaRepository userJpaRepository;
    private final JPAQueryFactory queryFactory;

    private static final QUserJpaEntity userJpaEntity = QUserJpaEntity.userJpaEntity;

    @Override
    public User save(User user) {
        UserJpaEntity entity = userJpaRepository.findById(user.getUserId())
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        entity.updateFromDomain(user);
        return entity.toDomain();
    }

    @Override
    public void updatePortfolioVisibility(Long userId, boolean portfolioPublic) {
        queryFactory
            .update(userJpaEntity)
            .set(userJpaEntity.portfolioPublic, portfolioPublic)
            .where(userJpaEntity.id.eq(userId))
            .execute();
    }
}
