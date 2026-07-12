package ksh.tryptobackend.user.application.port.in.dto.result;

import ksh.tryptobackend.user.domain.model.User;

public record KakaoLoginResult(Long userId, String nickname, boolean newUser, String sessionId) {

    public static KakaoLoginResult of(User user, boolean newUser, String sessionId) {
        return new KakaoLoginResult(user.getUserId(), user.getNickname().value(), newUser, sessionId);
    }
}
