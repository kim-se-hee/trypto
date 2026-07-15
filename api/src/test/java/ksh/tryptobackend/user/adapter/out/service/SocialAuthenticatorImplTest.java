package ksh.tryptobackend.user.adapter.out.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.adapter.out.oauth.OAuthClient;
import ksh.tryptobackend.user.domain.vo.ClientType;
import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SocialAuthenticatorImplTest {

    @Test
    @DisplayName("액세스 토큰 인증은 해당 제공자 클라이언트로 신원 조회를 위임한다")
    void authenticateWithAccessToken_delegatesToProviderClient() {
        SocialAuthenticatorImpl authenticator = new SocialAuthenticatorImpl(List.of(new StubKakaoClient()));

        SocialIdentity identity = authenticator.authenticateWithAccessToken(Provider.KAKAO, "app-access-token");

        assertThat(identity).isEqualTo(SocialIdentity.of(Provider.KAKAO, "app-access-token"));
    }

    @Test
    @DisplayName("등록되지 않은 제공자로 액세스 토큰 인증을 요청하면 미지원 제공자 예외를 던진다")
    void authenticateWithAccessToken_unknownProvider_throwsInvalidProvider() {
        SocialAuthenticatorImpl authenticator = new SocialAuthenticatorImpl(List.of(new StubKakaoClient()));

        assertThatThrownBy(() -> authenticator.authenticateWithAccessToken(Provider.GOOGLE, "app-access-token"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PROVIDER);
    }

    private static class StubKakaoClient implements OAuthClient {

        @Override
        public Provider provider() {
            return Provider.KAKAO;
        }

        @Override
        public SocialIdentity getIdentity(String authorizationCode, String codeVerifier, ClientType clientType) {
            return SocialIdentity.of(Provider.KAKAO, authorizationCode);
        }

        @Override
        public SocialIdentity getIdentityByAccessToken(String accessToken) {
            return SocialIdentity.of(Provider.KAKAO, accessToken);
        }
    }
}
