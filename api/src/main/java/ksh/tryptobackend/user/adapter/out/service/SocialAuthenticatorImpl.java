package ksh.tryptobackend.user.adapter.out.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.adapter.out.oauth.OAuthClient;
import ksh.tryptobackend.user.domain.service.SocialAuthenticator;
import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import org.springframework.stereotype.Component;

@Component
public class SocialAuthenticatorImpl implements SocialAuthenticator {

    private final Map<Provider, OAuthClient> clients;

    public SocialAuthenticatorImpl(List<OAuthClient> clients) {
        this.clients =
                clients.stream().collect(Collectors.toUnmodifiableMap(OAuthClient::provider, Function.identity()));
    }

    @Override
    public SocialIdentity authenticate(Provider provider, String authorizationCode, String codeVerifier) {
        OAuthClient client = clients.get(provider);
        if (client == null) {
            throw new CustomException(ErrorCode.INVALID_PROVIDER);
        }
        return client.getIdentity(authorizationCode, codeVerifier);
    }
}
