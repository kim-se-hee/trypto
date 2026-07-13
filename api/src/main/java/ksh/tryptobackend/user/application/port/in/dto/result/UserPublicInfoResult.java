package ksh.tryptobackend.user.application.port.in.dto.result;

import ksh.tryptobackend.user.domain.model.User;

public record UserPublicInfoResult(Long userId, String nickname) {

    public static UserPublicInfoResult from(User user) {
        return new UserPublicInfoResult(user.getUserId(), user.getNickname().value());
    }
}
