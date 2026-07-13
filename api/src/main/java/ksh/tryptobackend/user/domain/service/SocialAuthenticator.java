package ksh.tryptobackend.user.domain.service;

import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;

public interface SocialAuthenticator {

    SocialIdentity authenticate(Provider provider, String authorizationCode, String codeVerifier);
}
