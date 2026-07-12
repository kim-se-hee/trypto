package ksh.tryptobackend.user.domain.service;

import java.time.LocalDateTime;
import ksh.tryptobackend.user.domain.model.SocialAccount;
import ksh.tryptobackend.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountWithdrawalService {

    private final AnonymousNicknameGenerator anonymousNicknameGenerator;

    public void withdraw(User user, SocialAccount socialAccount, LocalDateTime now) {
        user.withdraw(anonymousNicknameGenerator.generate(), now);
        socialAccount.disconnect();
    }
}
