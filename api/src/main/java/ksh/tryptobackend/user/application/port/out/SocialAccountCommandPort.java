package ksh.tryptobackend.user.application.port.out;

import ksh.tryptobackend.user.domain.model.SocialAccount;

public interface SocialAccountCommandPort {

    SocialAccount register(SocialAccount socialAccount);

    void save(SocialAccount socialAccount);
}
