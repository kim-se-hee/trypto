package ksh.tryptobackend.user.adapter.out.service;

import java.security.SecureRandom;
import java.util.List;
import ksh.tryptobackend.user.adapter.out.persistence.entity.NicknameSequenceJpaEntity;
import ksh.tryptobackend.user.adapter.out.persistence.repository.NicknameSequenceJpaRepository;
import ksh.tryptobackend.user.domain.service.UniqueNicknameGenerator;
import ksh.tryptobackend.user.domain.vo.Nickname;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UniqueNicknameGeneratorImpl implements UniqueNicknameGenerator {

    private static final List<String> ADJECTIVES =
            List.of(
                    "구불한", "잔잔한", "성실한", "느긋한", "재빠른", "든든한", "포근한", "명랑한", "차분한", "용감한", "슬기로운",
                    "정직한", "우아한", "다정한", "당당한", "신중한");
    private static final String NOUN = "사용자";
    private static final String SUFFIX_FORMAT = "%04d";

    private final SecureRandom random = new SecureRandom();
    private final NicknameSequenceJpaRepository nicknameSequenceJpaRepository;

    @Override
    public Nickname generate() {
        long sequence =
                nicknameSequenceJpaRepository.save(NicknameSequenceJpaEntity.create()).getId();
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        return Nickname.of(adjective + NOUN + String.format(SUFFIX_FORMAT, sequence));
    }
}
