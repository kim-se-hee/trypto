package ksh.tryptobackend.user.adapter.in.dto.response;

import ksh.tryptobackend.user.domain.model.User;

public record ChangeNicknameResponse(Long userId, String nickname) {

    public static ChangeNicknameResponse from(User user) {
        return new ChangeNicknameResponse(user.getUserId(), user.getNickname().value());
    }
}
