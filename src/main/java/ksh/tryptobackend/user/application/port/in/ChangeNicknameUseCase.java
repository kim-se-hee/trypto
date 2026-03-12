package ksh.tryptobackend.user.application.port.in;

import ksh.tryptobackend.user.application.port.in.dto.command.ChangeNicknameCommand;
import ksh.tryptobackend.user.domain.model.User;

public interface ChangeNicknameUseCase {

    User changeNickname(ChangeNicknameCommand command);
}
