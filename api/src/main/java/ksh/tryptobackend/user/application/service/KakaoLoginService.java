package ksh.tryptobackend.user.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import ksh.tryptobackend.user.application.port.in.KakaoLoginUseCase;
import ksh.tryptobackend.user.application.port.in.dto.command.KakaoLoginCommand;
import ksh.tryptobackend.user.application.port.in.dto.result.KakaoLoginResult;
import ksh.tryptobackend.user.application.port.out.SessionCommandPort;
import ksh.tryptobackend.user.application.port.out.SocialIdentityQueryPort;
import ksh.tryptobackend.user.application.port.out.UserCommandPort;
import ksh.tryptobackend.user.application.port.out.UserQueryPort;
import ksh.tryptobackend.user.domain.model.User;
import ksh.tryptobackend.user.domain.service.UniqueNicknameGenerator;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KakaoLoginService implements KakaoLoginUseCase {

    private final SocialIdentityQueryPort socialIdentityQueryPort;
    private final UserQueryPort userQueryPort;
    private final UserCommandPort userCommandPort;
    private final SessionCommandPort sessionCommandPort;
    private final UniqueNicknameGenerator uniqueNicknameGenerator;
    private final Clock clock;

    @Override
    public KakaoLoginResult login(KakaoLoginCommand command) {
        SocialIdentity socialIdentity =
                socialIdentityQueryPort.getByAuthorizationCode(
                        command.code(), command.codeVerifier());

        Optional<User> existingUser = userQueryPort.findBySocialIdentity(socialIdentity);
        boolean newUser = existingUser.isEmpty();
        User user =
                existingUser.orElseGet(
                        () ->
                                userCommandPort.register(
                                        User.registerWith(
                                                socialIdentity,
                                                uniqueNicknameGenerator.generate(),
                                                LocalDateTime.now(clock))));

        String sessionId = sessionCommandPort.create(user.getUserId());
        return new KakaoLoginResult(
                user.getUserId(), user.getNickname().value(), newUser, sessionId);
    }
}
