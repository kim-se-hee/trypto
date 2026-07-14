package ksh.tryptobackend.user.adapter.out.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.oauth.google")
public class GoogleOAuthProperties extends OAuthProviderProperties {}
