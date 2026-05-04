package ksh.tryptobackend.user.adapter.in.dto.request;

import jakarta.validation.constraints.NotBlank;
import ksh.tryptobackend.user.application.port.in.dto.command.ChangeNicknameCommand;

public record ChangeNicknameRequest(@NotBlank String nickname) {

    public ChangeNicknameCommand toCommand(Long userId) {
        return new ChangeNicknameCommand(userId, nickname);
    }
}
