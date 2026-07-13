package ksh.tryptobackend.user.application.port.in;

import ksh.tryptobackend.user.application.port.in.dto.command.LoginCommand;
import ksh.tryptobackend.user.application.port.in.dto.result.LoginResult;

public interface LoginUseCase {

    LoginResult login(LoginCommand command);
}
