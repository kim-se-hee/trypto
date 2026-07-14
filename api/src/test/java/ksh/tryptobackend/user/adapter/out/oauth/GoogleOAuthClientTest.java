package ksh.tryptobackend.user.adapter.out.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import ksh.tryptobackend.user.domain.vo.ClientType;
import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GoogleOAuthClientTest {

    private static final String USER_INFO_RESPONSE = "{\"sub\":\"google-sub-1\"}";
    private static final String AUTHORIZATION_CODE = "authorization-code";
    private static final String CODE_VERIFIER = "code-verifier";

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
        return properties;
    }
}
