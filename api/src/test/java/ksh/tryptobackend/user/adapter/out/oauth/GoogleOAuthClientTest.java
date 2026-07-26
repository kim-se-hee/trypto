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

class GoogleOAuthClientTest {

    private static final String SUBJECT = "google-sub-1";
    private static final String USER_INFO_RESPONSE = "{\"sub\":\"" + SUBJECT + "\"}";
    private static final String AUTHORIZATION_CODE = "authorization-code";
    private static final String CODE_VERIFIER = "code-verifier";
    private static final String APP_ID_TOKEN = "app-id-token";
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    private static final OAuthCredentials WEB_CREDENTIALS =
            new OAuthCredentials("google-web-id", "google-web-secret", "http://localhost:5173/auth/google/callback");
    private static final OAuthCredentials ANDROID_CREDENTIALS =
            new OAuthCredentials("google-android-id", "", "trypto://auth/google/callback");
    private static final OAuthCredentials IOS_CREDENTIALS =
            new OAuthCredentials("google-ios-id", "", "trypto://auth/google/callback");

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
    @DisplayName("클라이언트 시크릿이 없는 안드로이드 자격증명은 토큰 교환 폼에서 client_secret 을 제외한다")
    void getIdentity_androidCredentialsWithoutClientSecret_omitsClientSecretFromForm() {
        GoogleOAuthClient client = new GoogleOAuthClient(configuredProperties());

        SocialIdentity identity = client.getIdentity(AUTHORIZATION_CODE, CODE_VERIFIER, ClientType.ANDROID);

        assertThat(identity).isEqualTo(SocialIdentity.of(Provider.GOOGLE, "google-sub-1"));
        assertThat(server.tokenRequestForm())
                .doesNotContainKey("client_secret")
                .containsEntry("client_id", ANDROID_CREDENTIALS.clientId())
                .containsEntry("redirect_uri", ANDROID_CREDENTIALS.redirectUri());
    }

    @Test
    @DisplayName("클라이언트 시크릿이 있는 웹 자격증명은 토큰 교환 폼에 client_secret 을 담는다")
    void getIdentity_webCredentialsWithClientSecret_sendsClientSecretInForm() {
        GoogleOAuthClient client = new GoogleOAuthClient(configuredProperties());

        client.getIdentity(AUTHORIZATION_CODE, CODE_VERIFIER, ClientType.WEB);

        assertThat(server.tokenRequestForm())
                .containsEntry("client_secret", WEB_CREDENTIALS.clientSecret())
                .containsEntry("client_id", WEB_CREDENTIALS.clientId())
                .containsEntry("redirect_uri", WEB_CREDENTIALS.redirectUri());
    }

    @Test
    @DisplayName("안드로이드와 아이폰은 서로 다른 자격증명으로 토큰을 교환한다")
    void getIdentity_androidAndIos_exchangeTokenWithOwnCredentials() {
        GoogleOAuthClient client = new GoogleOAuthClient(configuredProperties());

        client.getIdentity(AUTHORIZATION_CODE, CODE_VERIFIER, ClientType.ANDROID);
        Map<String, String> androidForm = server.tokenRequestForm();

        client.getIdentity(AUTHORIZATION_CODE, CODE_VERIFIER, ClientType.IOS);
        Map<String, String> iosForm = server.tokenRequestForm();

        assertThat(androidForm).containsEntry("client_id", ANDROID_CREDENTIALS.clientId());
        assertThat(iosForm).containsEntry("client_id", IOS_CREDENTIALS.clientId());
        assertThat(androidForm.get("client_id")).isNotEqualTo(iosForm.get("client_id"));
    }

    @Test
    @DisplayName("앱이 네이티브로 받은 ID 토큰은 발급자·발급 대상 확인 후 신원으로 바뀐다")
    void getIdentityByAccessToken_trustedIssuerAndOwnAudience_resolvesIdentity() {
        server.tokenInfoResponse(tokenInfo(GOOGLE_ISSUER, WEB_CREDENTIALS.clientId()));
        GoogleOAuthClient client = new GoogleOAuthClient(configuredProperties());

        SocialIdentity identity = client.getIdentityByAccessToken(APP_ID_TOKEN);

        assertThat(identity).isEqualTo(SocialIdentity.of(Provider.GOOGLE, SUBJECT));
    }

    @Test
    @DisplayName("구글이 발급하지 않은 ID 토큰은 로그인에 실패한다")
    void getIdentityByAccessToken_untrustedIssuer_throwsSocialLoginFailed() {
        server.tokenInfoResponse(tokenInfo("https://accounts.attacker.example", WEB_CREDENTIALS.clientId()));
        GoogleOAuthClient client = new GoogleOAuthClient(configuredProperties());

        assertThatThrownBy(() -> client.getIdentityByAccessToken(APP_ID_TOKEN))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SOCIAL_LOGIN_FAILED);
    }

    @Test
    @DisplayName("다른 앱에 발급된 ID 토큰은 로그인에 실패한다")
    void getIdentityByAccessToken_otherAppAudience_throwsSocialLoginFailed() {
        server.tokenInfoResponse(tokenInfo(GOOGLE_ISSUER, "another-app-client-id"));
        GoogleOAuthClient client = new GoogleOAuthClient(configuredProperties());

        assertThatThrownBy(() -> client.getIdentityByAccessToken(APP_ID_TOKEN))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SOCIAL_LOGIN_FAILED);
    }

    private String tokenInfo(String issuer, String audience) {
        return "{\"sub\":\"%s\",\"aud\":\"%s\",\"iss\":\"%s\"}".formatted(SUBJECT, audience, issuer);
    }

    private GoogleOAuthProperties configuredProperties() {
        return propertiesWith(WEB_CREDENTIALS, ANDROID_CREDENTIALS, IOS_CREDENTIALS);
    }

    private GoogleOAuthProperties propertiesWith(OAuthCredentials web, OAuthCredentials android, OAuthCredentials ios) {
        Map<ClientType, OAuthCredentials> credentials = new EnumMap<>(ClientType.class);
        credentials.put(ClientType.WEB, web);
        credentials.put(ClientType.ANDROID, android);
        credentials.put(ClientType.IOS, ios);

        GoogleOAuthProperties properties = new GoogleOAuthProperties();
        properties.setCredentials(credentials);
        properties.setTokenUri(server.tokenUri());
        properties.setUserInfoUri(server.userInfoUri());
        properties.setTokenInfoUri(server.tokenInfoUri());
        return properties;
    }
}
