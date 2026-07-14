package ksh.tryptobackend.user.adapter.out.oauth;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.domain.vo.ClientType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class OAuthProviderProperties {

    private Map<ClientType, OAuthCredentials> credentials = new EnumMap<>(ClientType.class);
    private String tokenUri;
    private String userInfoUri;
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(5);

    public OAuthCredentials credentialsFor(ClientType clientType) {
        OAuthCredentials selected = credentials.get(clientType);
        if (selected == null || !selected.isConfigured()) {
            throw new CustomException(ErrorCode.SOCIAL_LOGIN_NOT_CONFIGURED);
        }
        return selected;
    }
}
