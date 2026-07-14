package ksh.tryptobackend.user.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.application.port.in.LoginUseCase;
import ksh.tryptobackend.user.application.port.in.dto.command.LoginCommand;
import ksh.tryptobackend.user.application.port.in.dto.result.LoginResult;
import ksh.tryptobackend.user.application.port.out.SessionCommandPort;
import ksh.tryptobackend.user.application.port.out.SocialAccountCommandPort;
import ksh.tryptobackend.user.application.port.out.SocialAccountQueryPort;
import ksh.tryptobackend.user.application.port.out.UserCommandPort;
import ksh.tryptobackend.user.application.port.out.UserQueryPort;
import ksh.tryptobackend.user.domain.model.SocialAccount;
import ksh.tryptobackend.user.domain.model.User;
import ksh.tryptobackend.user.domain.service.SocialAuthenticator;
import ksh.tryptobackend.user.domain.service.UniqueNicknameGenerator;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

    private final SocialAuthenticator socialAuthenticator;
    private final SocialAccountQueryPort socialAccountQueryPort;
    private final SocialAccountCommandPort socialAccountCommandPort;
    private final UserQueryPort userQueryPort;
    private final UserCommandPort userCommandPort;
    private final SessionCommandPort sessionCommandPort;
    private final UniqueNicknameGenerator uniqueNicknameGenerator;
    private final Clock clock;

    @Override
    @Transactional
    public LoginResult login(LoginCommand command) {
        SocialIdentity identity = socialAuthenticator.authenticate(
                command.provider(), command.code(), command.codeVerifier(), command.clientType());
        LocalDateTime now = LocalDateTime.now(clock);

        SocialAccount account = socialAccountQueryPort
                .findByIdentity(identity)
                .orElseGet(() -> socialAccountCommandPort.register(SocialAccount.register(identity, now)));
        if (account.isConnected()) {
            User user = userQueryPort
                    .findById(account.getUserId())
                    .orElseThrow(() -> new CustomException(ErrorCode.SOCIAL_LOGIN_FAILED));
            return LoginResult.of(user, false, sessionCommandPort.create(user.getUserId()));
        }

        userQueryPort
                .findLatestWithdrawnBySocialAccountId(account.getId())
                .ifPresent(withdrawn -> User.ensureReSignupAllowed(withdrawn.getDeletedAt(), now));

        User newUser =
                userCommandPort.register(User.registerWith(account.getId(), uniqueNicknameGenerator.generate(), now));
        account.connectTo(newUser.getUserId());
        socialAccountCommandPort.save(account);
        return LoginResult.of(newUser, true, sessionCommandPort.create(newUser.getUserId()));
    }
}
