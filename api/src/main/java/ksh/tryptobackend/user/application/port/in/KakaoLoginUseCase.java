package ksh.tryptobackend.user.application.port.in;

import ksh.tryptobackend.user.application.port.in.dto.command.KakaoLoginCommand;
import ksh.tryptobackend.user.application.port.in.dto.result.KakaoLoginResult;

public interface KakaoLoginUseCase {

    KakaoLoginResult login(KakaoLoginCommand command);
}
