package ksh.tryptobackend.user.adapter.out.acl;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.application.port.out.SocialIdentityQueryPort;
import ksh.tryptobackend.user.domain.vo.Provider;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class UserAclSocialIdentityQueryAdapter implements SocialIdentityQueryPort {

    private static final String GRANT_TYPE = "authorization_code";

    private final KakaoOAuthProperties properties;
    private final RestClient restClient;

    public UserAclSocialIdentityQueryAdapter(
            KakaoOAuthProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public SocialIdentity getByAuthorizationCode(String authorizationCode, String codeVerifier) {
        String accessToken = exchangeAccessToken(authorizationCode, codeVerifier);
        Long memberId = fetchMemberId(accessToken);
        return SocialIdentity.of(Provider.KAKAO, String.valueOf(memberId));
    }

    private String exchangeAccessToken(String authorizationCode, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", GRANT_TYPE);
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("code", authorizationCode);
        form.add("code_verifier", codeVerifier);

        KakaoTokenResponse response = requestToken(form);
        if (response == null || response.accessToken() == null) {
            throw new CustomException(ErrorCode.SOCIAL_SERVER_ERROR);
        }
        return response.accessToken();
    }

    private KakaoTokenResponse requestToken(MultiValueMap<String, String> form) {
        try {
            return restClient
                    .post()
                    .uri(properties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::failAuthentication)
                    .onStatus(HttpStatusCode::is5xxServerError, this::failProviderServer)
                    .body(KakaoTokenResponse.class);
        } catch (RestClientException e) {
            throw new CustomException(ErrorCode.SOCIAL_SERVER_ERROR);
        }
    }

    private Long fetchMemberId(String accessToken) {
        KakaoUserResponse response = requestMember(accessToken);
        if (response == null || response.id() == null) {
            throw new CustomException(ErrorCode.SOCIAL_SERVER_ERROR);
        }
        return response.id();
    }

    private KakaoUserResponse requestMember(String accessToken) {
        try {
            return restClient
                    .get()
                    .uri(properties.getUserInfoUri())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::failAuthentication)
                    .onStatus(HttpStatusCode::is5xxServerError, this::failProviderServer)
                    .body(KakaoUserResponse.class);
        } catch (RestClientException e) {
            throw new CustomException(ErrorCode.SOCIAL_SERVER_ERROR);
        }
    }

    private void failAuthentication(HttpRequest request, ClientHttpResponse response) {
        throw new CustomException(ErrorCode.SOCIAL_LOGIN_FAILED);
    }

    private void failProviderServer(HttpRequest request, ClientHttpResponse response) {
        throw new CustomException(ErrorCode.SOCIAL_SERVER_ERROR);
    }
}
