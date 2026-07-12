package ksh.tryptobackend.user.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.application.port.in.DeleteAccountUseCase;
import ksh.tryptobackend.user.application.port.in.dto.command.DeleteAccountCommand;
import ksh.tryptobackend.user.application.port.out.SessionCommandPort;
import ksh.tryptobackend.user.application.port.out.SocialAccountCommandPort;
import ksh.tryptobackend.user.application.port.out.SocialAccountQueryPort;
import ksh.tryptobackend.user.application.port.out.UserCommandPort;
import ksh.tryptobackend.user.application.port.out.UserQueryPort;
import ksh.tryptobackend.user.domain.model.SocialAccount;
import ksh.tryptobackend.user.domain.model.User;
import ksh.tryptobackend.user.domain.service.AnonymousNicknameGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteAccountService implements DeleteAccountUseCase {

    private final UserQueryPort userQueryPort;
    private final UserCommandPort userCommandPort;
    private final SocialAccountQueryPort socialAccountQueryPort;
    private final SocialAccountCommandPort socialAccountCommandPort;
    private final SessionCommandPort sessionCommandPort;
    private final AnonymousNicknameGenerator anonymousNicknameGenerator;
    private final Clock clock;

    @Override
    @Transactional
    public void deleteAccount(DeleteAccountCommand command) {
        User user = userQueryPort
                .findById(command.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.withdraw(anonymousNicknameGenerator.generate(), LocalDateTime.now(clock));
        userCommandPort.save(user);

        SocialAccount socialAccount = socialAccountQueryPort.getById(user.getSocialAccountId());
        socialAccount.disconnect();
        socialAccountCommandPort.save(socialAccount);

        sessionCommandPort.deleteAllOf(user.getUserId());
    }
}
