package ksh.tryptobackend.user.adapter.out.oauth;

import java.net.http.HttpClient;
import java.time.Duration;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

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
