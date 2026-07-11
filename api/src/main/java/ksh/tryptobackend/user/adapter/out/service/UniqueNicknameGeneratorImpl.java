package ksh.tryptobackend.user.adapter.out.service;

import java.security.SecureRandom;
import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.application.port.out.UserQueryPort;
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
    private static final int SUFFIX_BOUND = 10000;
    private static final String SUFFIX_FORMAT = "%04d";
    private static final int MAX_ATTEMPTS = 20;

    private final SecureRandom random = new SecureRandom();
    private final UserQueryPort userQueryPort;

    @Override
    public Nickname generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Nickname candidate = randomCandidate();
            if (!userQueryPort.existsByNickname(candidate.value())) {
                return candidate;
            }
        }
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private Nickname randomCandidate() {
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String suffix = String.format(SUFFIX_FORMAT, random.nextInt(SUFFIX_BOUND));
        return Nickname.of(adjective + NOUN + suffix);
    }
}
