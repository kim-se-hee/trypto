package ksh.tryptobackend.user.adapter.out.oauth;

import java.net.http.HttpClient;
import java.time.Duration;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** 제공자 공통 HTTP 구성. 타임아웃 있는 RestClient 생성과 상태코드별 실패 변환을 담당한다. */
final class OAuthHttp {

    private OAuthHttp() {}

    static RestClient restClient(Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient =
                HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    static void failAuthentication(HttpRequest request, ClientHttpResponse response) {
        throw new CustomException(ErrorCode.SOCIAL_LOGIN_FAILED);
    }

    static void failProviderServer(HttpRequest request, ClientHttpResponse response) {
        throw new CustomException(ErrorCode.SOCIAL_SERVER_ERROR);
    }
}
