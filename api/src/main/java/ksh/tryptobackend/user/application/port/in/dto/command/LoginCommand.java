package ksh.tryptobackend.user.application.port.in.dto.command;

import ksh.tryptobackend.user.domain.vo.ClientType;
import ksh.tryptobackend.user.domain.vo.Provider;

public record LoginCommand(
        Provider provider, String code, String codeVerifier, ClientType clientType, String accessToken) {

    public boolean hasAccessToken() {
        return accessToken != null && !accessToken.isBlank();
    }
}
