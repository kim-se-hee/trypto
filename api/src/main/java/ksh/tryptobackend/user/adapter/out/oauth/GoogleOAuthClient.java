package ksh.tryptobackend.user.adapter.out.oauth;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GoogleOAuthClient implements OAuthClient {

    private static final String GRANT_TYPE = "authorization_code";

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
    public SocialIdentity getIdentity(String authorizationCode, String codeVerifier) {
        String accessToken = exchangeAccessToken(authorizationCode, codeVerifier);
        String memberId = fetchMemberId(accessToken);
        return SocialIdentity.of(Provider.GOOGLE, memberId);
    }

    private String exchangeAccessToken(String authorizationCode, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", GRANT_TYPE);
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("code", authorizationCode);
        form.add("code_verifier", codeVerifier);

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
