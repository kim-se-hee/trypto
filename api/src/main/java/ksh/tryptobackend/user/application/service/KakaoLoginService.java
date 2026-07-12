package ksh.tryptobackend.user.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.application.port.in.KakaoLoginUseCase;
import ksh.tryptobackend.user.application.port.in.dto.command.KakaoLoginCommand;
import ksh.tryptobackend.user.application.port.in.dto.result.KakaoLoginResult;
import ksh.tryptobackend.user.application.port.out.SessionCommandPort;
import ksh.tryptobackend.user.application.port.out.SocialAccountCommandPort;
import ksh.tryptobackend.user.application.port.out.SocialAccountQueryPort;
import ksh.tryptobackend.user.application.port.out.SocialIdentityQueryPort;
import ksh.tryptobackend.user.application.port.out.UserCommandPort;
import ksh.tryptobackend.user.application.port.out.UserQueryPort;
import ksh.tryptobackend.user.domain.model.SocialAccount;
import ksh.tryptobackend.user.domain.model.User;
import ksh.tryptobackend.user.domain.service.UniqueNicknameGenerator;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KakaoLoginService implements KakaoLoginUseCase {

    private final SocialIdentityQueryPort socialIdentityQueryPort;
    private final SocialAccountQueryPort socialAccountQueryPort;
    private final SocialAccountCommandPort socialAccountCommandPort;
    private final UserQueryPort userQueryPort;
    private final UserCommandPort userCommandPort;
    private final SessionCommandPort sessionCommandPort;
    private final UniqueNicknameGenerator uniqueNicknameGenerator;
    private final Clock clock;

    @Override
    public KakaoLoginResult login(KakaoLoginCommand command) {
        SocialIdentity identity =
                socialIdentityQueryPort.getByAuthorizationCode(command.code(), command.codeVerifier());
        LocalDateTime now = LocalDateTime.now(clock);

        SocialAccount account = socialAccountQueryPort
                .findByIdentity(identity)
                .orElseGet(() -> socialAccountCommandPort.register(SocialAccount.register(identity, now)));
        if (account.isConnected()) {
            User user = userQueryPort
                    .findById(account.getUserId())
                    .orElseThrow(() -> new CustomException(ErrorCode.SOCIAL_LOGIN_FAILED));
            return KakaoLoginResult.of(user, false, sessionCommandPort.create(user.getUserId()));
        }

        userQueryPort
                .findLatestWithdrawnBySocialAccountId(account.getId())
                .ifPresent(withdrawn -> User.ensureReSignupAllowed(withdrawn.getDeletedAt(), now));

        User newUser =
                userCommandPort.register(User.registerWith(account.getId(), uniqueNicknameGenerator.generate(), now));
        account.connectTo(newUser.getUserId());
        socialAccountCommandPort.save(account);
        return KakaoLoginResult.of(newUser, true, sessionCommandPort.create(newUser.getUserId()));
    }
}
