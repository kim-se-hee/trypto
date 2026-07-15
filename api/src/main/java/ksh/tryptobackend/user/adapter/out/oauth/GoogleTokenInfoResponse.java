package ksh.tryptobackend.user.adapter.out.oauth;

/**
 * 구글 tokeninfo 응답(ID 토큰 검증용). 서명·만료는 구글이 확인해 주고, 우리는 발급자(iss)와 발급 대상(aud)을
 * 대조한 뒤 사용자 식별자(sub)를 쓴다.
 */
public record GoogleTokenInfoResponse(String sub, String aud, String iss) {}
