package ksh.tryptobackend.common.seed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import ksh.tryptobackend.user.adapter.out.persistence.entity.UserJpaEntity;
import ksh.tryptobackend.user.adapter.out.persistence.repository.UserJpaRepository;
import ksh.tryptobackend.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
class UserDataSeeder {

    private final UserJpaRepository userRepository;

    @Transactional
    void seed(SeedContext ctx) {
        List<UserJpaEntity> entities = new ArrayList<>();

        entities.addAll(createMainUsers());
        entities.addAll(createBackgroundUsers());

        List<UserJpaEntity> saved = userRepository.saveAll(entities);
        saved.forEach(entity -> ctx.addUserId(entity.getNickname(), entity.getId()));

        log.info("[Seed] 사용자 {}명 생성 완료", saved.size());
    }

    private List<UserJpaEntity> createMainUsers() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                toEntity("김비트", true, now),
                toEntity("이더리움", false, now),
                toEntity("박솔라나", true, now),
                toEntity("최리플", true, now),
                toEntity("정도지", false, now),
                toEntity("한에이다", false, now),
                toEntity("강링크", false, now),
                toEntity("윤닷", true, now),
                toEntity("송아톰", false, now),
                toEntity("임앱트", true, now));
    }

    private List<UserJpaEntity> createBackgroundUsers() {
        LocalDateTime now = LocalDateTime.now();
        List<UserJpaEntity> users = new ArrayList<>();
        for (int i = 11; i <= 200; i++) {
            users.add(toEntity("투자자" + i, false, now));
        }
        return users;
    }

    private UserJpaEntity toEntity(String nickname, boolean portfolioPublic, LocalDateTime now) {
        User user = User.create(null, nickname, portfolioPublic, now);
        return UserJpaEntity.fromDomain(user);
    }
}
