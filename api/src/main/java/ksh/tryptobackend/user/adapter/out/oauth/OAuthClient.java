package ksh.tryptobackend.user.adapter.out.oauth;

import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;

/** 소셜 제공자별 OAuth 연동. 인가 코드를 토큰으로 교환하고 제공자 회원번호를 조회해 소셜 신원으로 번역한다. */
public interface OAuthClient {

    Provider provider();

    SocialIdentity getIdentity(String authorizationCode, String codeVerifier);
}
