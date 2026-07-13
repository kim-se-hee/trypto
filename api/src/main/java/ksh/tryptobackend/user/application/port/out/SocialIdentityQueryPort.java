package ksh.tryptobackend.user.application.port.out;

import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;

public interface SocialIdentityQueryPort {

    SocialIdentity getByAuthorizationCode(Provider provider, String authorizationCode, String codeVerifier);
}
