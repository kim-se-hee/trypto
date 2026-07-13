package ksh.tryptobackend.user.application.port.in.dto.command;

import ksh.tryptobackend.user.domain.vo.Provider;

public record LoginCommand(Provider provider, String code, String codeVerifier) {}
