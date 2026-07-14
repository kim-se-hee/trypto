package ksh.tryptobackend.user.adapter.out.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.domain.vo.ClientType;
import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KakaoOAuthClientTest {

    private static final String USER_INFO_RESPONSE = "{\"id\":1234}";
    private static final String AUTHORIZATION_CODE = "authorization-code";
    private static final String CODE_VERIFIER = "code-verifier";

    private static final OAuthCredentials WEB_CREDENTIALS =
            new OAuthCredentials("kakao-web-id", "kakao-web-secret", "http://localhost:5173/auth/kakao/callback");
    private static final OAuthCredentials MOBILE_CREDENTIALS =
            new OAuthCredentials("kakao-mobile-id", "kakao-mobile-secret", "trypto://auth/kakao/callback");

    private FakeOAuthServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = FakeOAuthServer.start(USER_INFO_RESPONSE);
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    @Test
    @DisplayName("웹 클라이언트로 인증하면 웹 자격증명으로 토큰을 교환한다")
    void getIdentity_webClientType_exchangesTokenWithWebCredentials() {
        KakaoOAuthClient client = new KakaoOAuthClient(propertiesWith(WEB_CREDENTIALS, MOBILE_CREDENTIALS));

        SocialIdentity identity = client.getIdentity(AUTHORIZATION_CODE, CODE_VERIFIER, ClientType.WEB);

        assertThat(identity).isEqualTo(SocialIdentity.of(Provider.KAKAO, "1234"));
        assertThat(server.tokenRequestForm())
                .containsEntry("grant_type", "authorization_code")
                .containsEntry("client_id", WEB_CREDENTIALS.clientId())
                .containsEntry("client_secret", WEB_CREDENTIALS.clientSecret())
                .containsEntry("redirect_uri", WEB_CREDENTIALS.redirectUri())
                .containsEntry("code", AUTHORIZATION_CODE)
                .containsEntry("code_verifier", CODE_VERIFIER);
    }

    @Test
    @DisplayName("모바일 클라이언트로 인증하면 모바일 자격증명으로 토큰을 교환한다")
    void getIdentity_mobileClientType_exchangesTokenWithMobileCredentials() {
        KakaoOAuthClient client = new KakaoOAuthClient(propertiesWith(WEB_CREDENTIALS, MOBILE_CREDENTIALS));

        client.getIdentity(AUTHORIZATION_CODE, CODE_VERIFIER, ClientType.MOBILE);

        assertThat(server.tokenRequestForm())
                .containsEntry("client_id", MOBILE_CREDENTIALS.clientId())
                .containsEntry("client_secret", MOBILE_CREDENTIALS.clientSecret())
                .containsEntry("redirect_uri", MOBILE_CREDENTIALS.redirectUri());
    }

    @Test
    @DisplayName("모바일 자격증명이 설정되지 않으면 설정 누락 예외를 던진다")
    void getIdentity_mobileCredentialsNotConfigured_throwsNotConfigured() {
        OAuthCredentials empty = new OAuthCredentials("", "", "");
        KakaoOAuthClient client = new KakaoOAuthClient(propertiesWith(WEB_CREDENTIALS, empty));

        assertThatThrownBy(() -> client.getIdentity(AUTHORIZATION_CODE, CODE_VERIFIER, ClientType.MOBILE))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SOCIAL_LOGIN_NOT_CONFIGURED);
        assertThat(server.tokenRequestForm()).isEmpty();
    }

    private KakaoOAuthProperties propertiesWith(OAuthCredentials web, OAuthCredentials mobile) {
        Map<ClientType, OAuthCredentials> credentials = new EnumMap<>(ClientType.class);
        credentials.put(ClientType.WEB, web);
        credentials.put(ClientType.MOBILE, mobile);

        KakaoOAuthProperties properties = new KakaoOAuthProperties();
        properties.setCredentials(credentials);
        properties.setTokenUri(server.tokenUri());
        properties.setUserInfoUri(server.userInfoUri());
        return properties;
    }
}
