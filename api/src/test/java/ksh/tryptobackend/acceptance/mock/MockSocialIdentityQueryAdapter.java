package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.user.application.port.out.SocialIdentityQueryPort;
import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;

/** 제공자 토큰 교환을 대체하는 목. 인가 코드를 그대로 제공자 회원번호로 매핑한다. */
public class MockSocialIdentityQueryAdapter implements SocialIdentityQueryPort {

    @Override
    public SocialIdentity getByAuthorizationCode(Provider provider, String authorizationCode, String codeVerifier) {
        return SocialIdentity.of(provider, authorizationCode);
    }
}
