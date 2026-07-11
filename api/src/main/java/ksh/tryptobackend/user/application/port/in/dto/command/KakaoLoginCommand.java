package ksh.tryptobackend.user.application.port.in.dto.command;

public record KakaoLoginCommand(String code, String codeVerifier) {}
