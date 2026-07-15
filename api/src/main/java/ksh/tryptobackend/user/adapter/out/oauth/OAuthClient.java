package ksh.tryptobackend.user.adapter.out.oauth;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.domain.vo.ClientType;
import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;

public interface OAuthClient {

    Provider provider();

    SocialIdentity getIdentity(String authorizationCode, String codeVerifier, ClientType clientType);

    default SocialIdentity getIdentityByAccessToken(String accessToken) {
        throw new CustomException(ErrorCode.ACCESS_TOKEN_LOGIN_NOT_SUPPORTED);
    }
}
