package ksh.tryptobackend.user.adapter.in.dto.response;

import ksh.tryptobackend.user.application.port.in.dto.result.KakaoLoginResult;

public record KakaoLoginResponse(Long userId, String nickname, boolean newUser) {

    public static KakaoLoginResponse from(KakaoLoginResult result) {
        return new KakaoLoginResponse(result.userId(), result.nickname(), result.newUser());
    }
}
