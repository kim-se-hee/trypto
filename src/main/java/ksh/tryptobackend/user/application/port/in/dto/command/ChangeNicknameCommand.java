package ksh.tryptobackend.user.application.port.in.dto.command;

public record ChangeNicknameCommand(
    Long userId,
    String nickname
) {
}
