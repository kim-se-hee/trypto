package ksh.tryptobackend.user.adapter.in.dto.request;

import jakarta.validation.constraints.AssertTrue;
import ksh.tryptobackend.user.application.port.in.dto.command.LoginCommand;
import ksh.tryptobackend.user.domain.vo.ClientType;
import ksh.tryptobackend.user.domain.vo.Provider;

public record LoginRequest(String code, String codeVerifier, String accessToken, String clientType) {

    @AssertTrue(message = "인가 코드와 검증값 또는 액세스 토큰 중 하나는 있어야 합니다.") public boolean isCredentialProvided() {
        return hasAuthorizationCode() || hasAccessToken();
    }

    public LoginCommand toCommand(Provider provider) {
        return new LoginCommand(provider, code, codeVerifier, ClientType.fromNullable(clientType), accessToken);
    }

    private boolean hasAuthorizationCode() {
        return isPresent(code) && isPresent(codeVerifier);
    }

    private boolean hasAccessToken() {
        return isPresent(accessToken);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
