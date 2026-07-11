package ksh.tryptobackend.user.application.port.out;

import ksh.tryptobackend.user.domain.vo.SocialIdentity;

public interface SocialIdentityQueryPort {

    SocialIdentity getByAuthorizationCode(String authorizationCode, String codeVerifier);
}
