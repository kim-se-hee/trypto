package ksh.tryptobackend.user.adapter.out.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.oauth.kakao")
public class KakaoOAuthProperties extends OAuthProviderProperties {}
