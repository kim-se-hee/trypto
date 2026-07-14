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
    private static final OAuthCredentials ANDROID_CREDENTIALS =
            new OAuthCredentials("kakao-android-id", "kakao-android-secret", "trypto://auth/kakao/callback");
    private static final OAuthCredentials IOS_CREDENTIALS =
            new OAuthCredentials("kakao-ios-id", "kakao-ios-secret", "trypto://auth/kakao/callback");

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
        KakaoOAuthClient client = new KakaoOAuthClient(configuredProperties());

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
    @DisplayName("안드로이드 클라이언트로 인증하면 안드로이드 자격증명으로 토큰을 교환한다")
    void getIdentity_androidClientType_exchangesTokenWithAndroidCredentials() {
        KakaoOAuthClient client = new KakaoOAuthClient(configuredProperties());

        client.getIdentity(AUTHORIZATION_CODE, CODE_VERIFIER, ClientType.ANDROID);

        assertThat(server.tokenRequestForm())
                .containsEntry("client_id", ANDROID_CREDENTIALS.clientId())
                .containsEntry("client_secret", ANDROID_CREDENTIALS.clientSecret())
                .containsEntry("redirect_uri", ANDROID_CREDENTIALS.redirectUri());
    }

    @Test
    @DisplayName("안드로이드 자격증명이 설정되지 않으면 설정 누락 예외를 던진다")
    void getIdentity_androidCredentialsNotConfigured_throwsNotConfigured() {
        OAuthCredentials empty = new OAuthCredentials("", "", "");
        KakaoOAuthClient client = new KakaoOAuthClient(propertiesWith(WEB_CREDENTIALS, empty, IOS_CREDENTIALS));

        assertThatThrownBy(() -> client.getIdentity(AUTHORIZATION_CODE, CODE_VERIFIER, ClientType.ANDROID))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SOCIAL_LOGIN_NOT_CONFIGURED);
        assertThat(server.tokenRequestForm()).isEmpty();
    }

    private KakaoOAuthProperties configuredProperties() {
        return propertiesWith(WEB_CREDENTIALS, ANDROID_CREDENTIALS, IOS_CREDENTIALS);
    }

    private KakaoOAuthProperties propertiesWith(OAuthCredentials web, OAuthCredentials android, OAuthCredentials ios) {
        Map<ClientType, OAuthCredentials> credentials = new EnumMap<>(ClientType.class);
        credentials.put(ClientType.WEB, web);
        credentials.put(ClientType.ANDROID, android);
        credentials.put(ClientType.IOS, ios);

        KakaoOAuthProperties properties = new KakaoOAuthProperties();
        properties.setCredentials(credentials);
        properties.setTokenUri(server.tokenUri());
        properties.setUserInfoUri(server.userInfoUri());
        return properties;
    }
}
