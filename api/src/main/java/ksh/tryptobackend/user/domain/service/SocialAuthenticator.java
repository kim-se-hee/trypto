package ksh.tryptobackend.user.domain.service;

import ksh.tryptobackend.user.domain.vo.ClientType;
import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;

public interface SocialAuthenticator {

    SocialIdentity authenticate(
            Provider provider, String authorizationCode, String codeVerifier, ClientType clientType);

    SocialIdentity authenticateWithAccessToken(Provider provider, String accessToken);
}
