package ksh.tryptobackend.common.seed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import ksh.tryptobackend.user.adapter.out.persistence.entity.SocialAccountJpaEntity;
import ksh.tryptobackend.user.adapter.out.persistence.entity.UserJpaEntity;
import ksh.tryptobackend.user.adapter.out.persistence.repository.SocialAccountJpaRepository;
import ksh.tryptobackend.user.adapter.out.persistence.repository.UserJpaRepository;
import ksh.tryptobackend.user.domain.model.SocialAccount;
import ksh.tryptobackend.user.domain.model.User;
import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
class UserDataSeeder {

    private final UserJpaRepository userRepository;
    private final SocialAccountJpaRepository socialAccountRepository;

    @Transactional
    void seed(SeedContext ctx) {
        LocalDateTime now = LocalDateTime.now();
        List<UserSpec> specs = new ArrayList<>();
        specs.addAll(mainUserSpecs());
        specs.addAll(backgroundUserSpecs());

        for (UserSpec spec : specs) {
            Long userId = createUser(spec, now);
            ctx.addUserId(spec.nickname(), userId);
        }

        log.info("[Seed] 사용자 {}명 생성 완료", specs.size());
    }

    private Long createUser(UserSpec spec, LocalDateTime now) {
        SocialIdentity identity = SocialIdentity.of(Provider.KAKAO, "seed-" + spec.nickname());
        SocialAccountJpaEntity account =
                socialAccountRepository.save(SocialAccountJpaEntity.fromDomain(SocialAccount.register(identity, now)));
        UserJpaEntity user = userRepository.save(
                UserJpaEntity.fromDomain(User.create(account.getId(), spec.nickname(), spec.portfolioPublic(), now)));
        account.connectTo(user.getId());
        socialAccountRepository.save(account);
        return user.getId();
    }

    private List<UserSpec> mainUserSpecs() {
        return List.of(
                new UserSpec("김비트", true),
                new UserSpec("이더리움", false),
                new UserSpec("박솔라나", true),
                new UserSpec("최리플", true),
                new UserSpec("정도지", false),
                new UserSpec("한에이다", false),
                new UserSpec("강링크", false),
                new UserSpec("윤닷", true),
                new UserSpec("송아톰", false),
                new UserSpec("임앱트", true));
    }

    private List<UserSpec> backgroundUserSpecs() {
        List<UserSpec> specs = new ArrayList<>();
        for (int i = 11; i <= 200; i++) {
            specs.add(new UserSpec("투자자" + i, false));
        }
        return specs;
    }

    private record UserSpec(String nickname, boolean portfolioPublic) {}
}
