package ksh.tryptobackend.user.adapter.out.oauth;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

final class OAuthHttp {

    private static final Logger log = LoggerFactory.getLogger(OAuthHttp.class);
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
        logProviderFailure(request, response);
        throw new CustomException(ErrorCode.SOCIAL_LOGIN_FAILED);
    }

    static void failProviderServer(HttpRequest request, ClientHttpResponse response) {
        logProviderFailure(request, response);
        throw new CustomException(ErrorCode.SOCIAL_SERVER_ERROR);
    }

    // 제공자(카카오/구글)가 돌려준 실패 응답을 남긴다. 이 로그가 없으면 SOCIAL_LOGIN_FAILED 의
    // 실제 원인(redirect_uri 불일치·client_secret 오류·인가 코드 만료 등)을 운영에서 알 수 없다.
    // 실패 응답 본문에는 에러 코드만 담기며 액세스 토큰은 들어 있지 않다.
    private static void logProviderFailure(HttpRequest request, ClientHttpResponse response) {
        try {
            log.warn(
                    "소셜 로그인 제공자 요청 실패: {} {} -> status={} body={}",
                    request.getMethod(),
                    request.getURI(),
                    response.getStatusCode(),
                    StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("소셜 로그인 제공자 요청 실패, 응답 본문을 읽지 못했습니다", e);
        }
    }
}
