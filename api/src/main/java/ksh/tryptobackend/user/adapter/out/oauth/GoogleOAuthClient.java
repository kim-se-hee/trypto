package ksh.tryptobackend.user.adapter.out.oauth;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.domain.vo.ClientType;
import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GoogleOAuthClient implements OAuthClient {

    private final GoogleOAuthProperties properties;
    private final RestClient restClient;

    public GoogleOAuthClient(GoogleOAuthProperties properties) {
        this.properties = properties;
        this.restClient = OAuthHttp.restClient(properties.getConnectTimeout(), properties.getReadTimeout());
    }

    @Override
    public Provider provider() {
        return Provider.GOOGLE;
    }

    @Override
    public SocialIdentity getIdentity(String authorizationCode, String codeVerifier, ClientType clientType) {
        String accessToken = exchangeAccessToken(authorizationCode, codeVerifier, clientType);
        String memberId = fetchMemberId(accessToken);
        return SocialIdentity.of(Provider.GOOGLE, memberId);
    }

    private String exchangeAccessToken(String authorizationCode, String codeVerifier, ClientType clientType) {
        MultiValueMap<String, String> form =
                OAuthHttp.authorizationCodeForm(properties.credentialsFor(clientType), authorizationCode, codeVerifier);

        GoogleTokenResponse response = requestToken(form);
        if (response == null || response.accessToken() == null) {
            throw new CustomException(ErrorCode.SOCIAL_SERVER_ERROR);
        }
        return response.accessToken();
    }

    private GoogleTokenResponse requestToken(MultiValueMap<String, String> form) {
        try {
            return restClient
                    .post()
                    .uri(properties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, OAuthHttp::failAuthentication)
                    .onStatus(HttpStatusCode::is5xxServerError, OAuthHttp::failProviderServer)
                    .body(GoogleTokenResponse.class);
        } catch (RestClientException e) {
            throw new CustomException(ErrorCode.SOCIAL_SERVER_ERROR);
        }
    }

    private String fetchMemberId(String accessToken) {
        GoogleUserResponse response = requestMember(accessToken);
        if (response == null || response.sub() == null) {
            throw new CustomException(ErrorCode.SOCIAL_SERVER_ERROR);
        }
        return response.sub();
    }

    private GoogleUserResponse requestMember(String accessToken) {
        try {
            return restClient
                    .get()
                    .uri(properties.getUserInfoUri())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, OAuthHttp::failAuthentication)
                    .onStatus(HttpStatusCode::is5xxServerError, OAuthHttp::failProviderServer)
                    .body(GoogleUserResponse.class);
        } catch (RestClientException e) {
            throw new CustomException(ErrorCode.SOCIAL_SERVER_ERROR);
        }
    }
}
