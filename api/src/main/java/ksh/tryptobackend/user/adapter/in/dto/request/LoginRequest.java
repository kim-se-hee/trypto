package ksh.tryptobackend.user.adapter.in.dto.request;

import jakarta.validation.constraints.NotBlank;
import ksh.tryptobackend.user.application.port.in.dto.command.LoginCommand;
import ksh.tryptobackend.user.domain.vo.ClientType;
import ksh.tryptobackend.user.domain.vo.Provider;

public record LoginRequest(@NotBlank String code, @NotBlank String codeVerifier, String clientType) {

    public LoginCommand toCommand(Provider provider) {
        return new LoginCommand(provider, code, codeVerifier, ClientType.fromNullable(clientType));
    }
}
