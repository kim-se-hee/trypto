package ksh.tryptobackend.user.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
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

        Optional<SocialAccount> existing = socialAccountQueryPort.findByIdentity(identity);
        if (existing.isPresent() && existing.get().isConnected()) {
            return buildResult(loadUser(existing.get().getUserId()), false);
        }

        SocialAccount account =
                existing.orElseGet(() -> socialAccountCommandPort.register(SocialAccount.register(identity, now)));
        if (account.isConnected()) {
            return buildResult(loadUser(account.getUserId()), false);
        }

        userQueryPort
                .findLatestWithdrawnBySocialAccountId(account.getId())
                .ifPresent(withdrawn -> User.ensureReSignupAllowed(withdrawn.getDeletedAt(), now));

        User newUser =
                userCommandPort.register(User.registerWith(account.getId(), uniqueNicknameGenerator.generate(), now));
        account.connectTo(newUser.getUserId());
        socialAccountCommandPort.save(account);
        return buildResult(newUser, true);
    }

    private User loadUser(Long userId) {
        return userQueryPort.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.SOCIAL_LOGIN_FAILED));
    }

    private KakaoLoginResult buildResult(User user, boolean newUser) {
        String sessionId = sessionCommandPort.create(user.getUserId());
        return new KakaoLoginResult(user.getUserId(), user.getNickname().value(), newUser, sessionId);
    }
}
