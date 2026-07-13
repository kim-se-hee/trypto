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
        List<String> nicknames = new ArrayList<>();
        nicknames.addAll(mainUserNicknames());
        nicknames.addAll(backgroundUserNicknames());

        for (String nickname : nicknames) {
            Long userId = createUser(nickname, now);
            ctx.addUserId(nickname, userId);
        }

        log.info("[Seed] 사용자 {}명 생성 완료", nicknames.size());
    }

    private Long createUser(String nickname, LocalDateTime now) {
        SocialIdentity identity = SocialIdentity.of(Provider.KAKAO, "seed-" + nickname);
        SocialAccountJpaEntity account =
                socialAccountRepository.save(SocialAccountJpaEntity.fromDomain(SocialAccount.register(identity, now)));
        UserJpaEntity user = userRepository.save(UserJpaEntity.fromDomain(User.create(account.getId(), nickname, now)));
        account.connectTo(user.getId());
        socialAccountRepository.save(account);
        return user.getId();
    }

    private List<String> mainUserNicknames() {
        return List.of("김비트", "이더리움", "박솔라나", "최리플", "정도지", "한에이다", "강링크", "윤닷", "송아톰", "임앱트");
    }

    private List<String> backgroundUserNicknames() {
        List<String> nicknames = new ArrayList<>();
        for (int i = 11; i <= 200; i++) {
            nicknames.add("투자자" + i);
        }
        return nicknames;
    }
}
