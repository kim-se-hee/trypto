package ksh.tryptobackend.user.adapter.in.dto.request;

import jakarta.validation.constraints.NotBlank;
import ksh.tryptobackend.user.application.port.in.dto.command.KakaoLoginCommand;

public record KakaoLoginRequest(
        @NotBlank String code, @NotBlank String codeVerifier) {

    public KakaoLoginCommand toCommand() {
        return new KakaoLoginCommand(code, codeVerifier);
    }
}
