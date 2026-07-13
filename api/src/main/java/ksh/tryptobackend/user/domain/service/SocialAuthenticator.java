package ksh.tryptobackend.user.domain.service;

import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;

/** 소셜 제공자 인증. 인가 코드를 검증해 인증된 사용자의 소셜 신원을 확인한다. 연동형 도메인 서비스로, 구현은 어댑터에 있다. */
public interface SocialAuthenticator {

    SocialIdentity authenticate(Provider provider, String authorizationCode, String codeVerifier);
}
