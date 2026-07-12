package ksh.tryptobackend.user.application.port.out;

import java.util.Optional;
import ksh.tryptobackend.user.domain.model.SocialAccount;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;

public interface SocialAccountQueryPort {

    Optional<SocialAccount> findByIdentity(SocialIdentity socialIdentity);
}
