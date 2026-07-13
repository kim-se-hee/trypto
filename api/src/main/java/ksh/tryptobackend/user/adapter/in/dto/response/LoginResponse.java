package ksh.tryptobackend.user.adapter.in.dto.response;

import ksh.tryptobackend.user.application.port.in.dto.result.LoginResult;

public record LoginResponse(Long userId, String nickname, boolean newUser) {

    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(result.userId(), result.nickname(), result.newUser());
    }
}
