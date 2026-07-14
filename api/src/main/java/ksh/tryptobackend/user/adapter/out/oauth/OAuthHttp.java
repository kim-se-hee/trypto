package ksh.tryptobackend.user.adapter.out.oauth;

import java.net.http.HttpClient;
import java.time.Duration;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

final class OAuthHttp {

    private static final String GRANT_TYPE = "authorization_code";

    private OAuthHttp() {}

    static RestClient restClient(Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient =
                HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    static MultiValueMap<String, String> authorizationCodeForm(
            OAuthCredentials credentials, String authorizationCode, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", GRANT_TYPE);
        form.add("client_id", credentials.clientId());
        if (credentials.hasClientSecret()) {
            form.add("client_secret", credentials.clientSecret());
        }
        form.add("redirect_uri", credentials.redirectUri());
        form.add("code", authorizationCode);
        form.add("code_verifier", codeVerifier);
        return form;
    }

    static void failAuthentication(HttpRequest request, ClientHttpResponse response) {
        throw new CustomException(ErrorCode.SOCIAL_LOGIN_FAILED);
    }

    static void failProviderServer(HttpRequest request, ClientHttpResponse response) {
        throw new CustomException(ErrorCode.SOCIAL_SERVER_ERROR);
    }
}
