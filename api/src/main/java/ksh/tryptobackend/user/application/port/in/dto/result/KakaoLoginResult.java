package ksh.tryptobackend.user.application.port.in.dto.result;

public record KakaoLoginResult(Long userId, String nickname, boolean newUser, String sessionId) {}
