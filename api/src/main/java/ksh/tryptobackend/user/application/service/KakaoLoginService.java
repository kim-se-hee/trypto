package ksh.tryptobackend.user.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.application.port.in.KakaoLoginUseCase;
import ksh.tryptobackend.user.application.port.in.dto.command.KakaoLoginCommand;
import ksh.tryptobackend.user.application.port.in.dto.result.KakaoLoginResult;
import ksh.tryptobackend.user.application.port.out.SessionCommandPort;
import ksh.tryptobackend.user.application.port.out.SocialIdentityQueryPort;
import ksh.tryptobackend.user.application.port.out.UserCommandPort;
import ksh.tryptobackend.user.application.port.out.UserQueryPort;
import ksh.tryptobackend.user.domain.model.User;
import ksh.tryptobackend.user.domain.service.UniqueNicknameGenerator;
import ksh.tryptobackend.user.domain.vo.Nickname;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

        return userQueryPort
                .findBySocialIdentity(socialIdentity)
                .map(user -> issueSession(user, false))
                .orElseGet(() -> registerAndIssueSession(socialIdentity));
    }

    private KakaoLoginResult registerAndIssueSession(SocialIdentity socialIdentity) {
        Nickname nickname = uniqueNicknameGenerator.generate();
        User newUser = User.registerWith(socialIdentity, nickname, LocalDateTime.now(clock));
        try {
            return issueSession(userCommandPort.save(newUser), true);
        } catch (DataIntegrityViolationException e) {
            User existing =
                    userQueryPort
                            .findBySocialIdentity(socialIdentity)
                            .orElseThrow(() -> new CustomException(ErrorCode.SOCIAL_LOGIN_FAILED));
            return issueSession(existing, false);
        }
    }

    private KakaoLoginResult issueSession(User user, boolean newUser) {
        String sessionId = sessionCommandPort.create(user.getUserId());
        return new KakaoLoginResult(
                user.getUserId(), user.getNickname().value(), newUser, sessionId);
    }
}
