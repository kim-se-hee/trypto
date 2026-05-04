package ksh.tryptobackend.user.application.port.in.dto.result;

public record UserPublicInfoResult(Long userId, String nickname, boolean portfolioPublic) {}
