package ksh.tryptobackend.user.adapter.out.oauth;

import java.util.Objects;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.oauth.google")
public class GoogleOAuthProperties extends OAuthProviderProperties {

    /** 구글이 발급자를 표기하는 두 형태를 모두 허용한다. */
    private static final Set<String> TRUSTED_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    /** ID 토큰 검증 엔드포인트. 앱(google_sign_in)이 네이티브로 받은 ID 토큰의 서명·만료를 구글이 확인해 준다. */
    private String tokenInfoUri = "https://oauth2.googleapis.com/tokeninfo";

    public String getTokenInfoUri() {
        return tokenInfoUri;
    }

    public void setTokenInfoUri(String tokenInfoUri) {
        this.tokenInfoUri = tokenInfoUri;
    }

    public boolean isTrustedIssuer(String iss) {
        return TRUSTED_ISSUERS.contains(iss);
    }

    /**
     * ID 토큰의 aud 가 이 프로젝트에 발급된 클라이언트 ID 중 하나인지 확인한다. 앱은 웹 클라이언트 ID 를
     * serverClientId 로 넘기므로 그 값이 aud 로 온다 — 다른 앱이 발급받은 토큰을 이 검사에서 걸러 낸다.
     */
    public boolean isOwnAudience(String aud) {
        if (aud == null || aud.isBlank()) {
            return false;
        }
        return getCredentials().values().stream()
                .map(OAuthCredentials::clientId)
                .filter(Objects::nonNull)
                .anyMatch(aud::equals);
    }
}
