package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.user.domain.service.SocialAuthenticator;
import ksh.tryptobackend.user.domain.vo.ClientType;
import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;

/** 제공자 인증을 대체하는 목. 인가 코드를 그대로 제공자 회원번호로 매핑하고, 인증에 쓰인 클라이언트 유형을 기록한다. */
public class MockSocialAuthenticator implements SocialAuthenticator {

    private ClientType lastClientType;

    @Override
    public SocialIdentity authenticate(
            Provider provider, String authorizationCode, String codeVerifier, ClientType clientType) {
        this.lastClientType = clientType;
        return SocialIdentity.of(provider, authorizationCode);
    }

    public ClientType getLastClientType() {
        return lastClientType;
    }
}
