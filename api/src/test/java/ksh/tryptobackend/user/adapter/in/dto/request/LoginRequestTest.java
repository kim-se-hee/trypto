package ksh.tryptobackend.user.adapter.in.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoginRequestTest {

    @Test
    @DisplayName("액세스 토큰만 있어도 자격 증명이 갖춰진 것으로 본다")
    void isCredentialProvided_accessTokenOnly_true() {
        LoginRequest request = new LoginRequest(null, null, "app-access-token", "android");

        assertThat(request.isCredentialProvided()).isTrue();
    }

    @Test
    @DisplayName("인가 코드와 검증값이 모두 있으면 자격 증명이 갖춰진 것으로 본다")
    void isCredentialProvided_codeAndVerifier_true() {
        LoginRequest request = new LoginRequest("code", "verifier", null, "web");

        assertThat(request.isCredentialProvided()).isTrue();
    }

    @Test
    @DisplayName("인가 코드만 있고 검증값이 없으면 자격 증명이 갖춰지지 않은 것으로 본다")
    void isCredentialProvided_codeWithoutVerifier_false() {
        LoginRequest request = new LoginRequest("code", null, null, "web");

        assertThat(request.isCredentialProvided()).isFalse();
    }

    @Test
    @DisplayName("어떤 자격 증명도 없으면 자격 증명이 갖춰지지 않은 것으로 본다")
    void isCredentialProvided_nothing_false() {
        LoginRequest request = new LoginRequest(null, null, null, null);

        assertThat(request.isCredentialProvided()).isFalse();
    }
}
