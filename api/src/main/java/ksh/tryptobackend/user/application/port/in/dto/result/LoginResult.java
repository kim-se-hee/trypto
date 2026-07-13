package ksh.tryptobackend.user.application.port.in.dto.result;

import ksh.tryptobackend.user.domain.model.User;

public record LoginResult(Long userId, String nickname, boolean newUser, String sessionId) {

    public static LoginResult of(User user, boolean newUser, String sessionId) {
        return new LoginResult(user.getUserId(), user.getNickname().value(), newUser, sessionId);
    }
}
