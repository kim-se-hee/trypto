package ksh.tryptobackend.user.adapter.out.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.application.port.out.UserQueryPort;
import ksh.tryptobackend.user.domain.service.NicknameUniquenessChecker;
import ksh.tryptobackend.user.domain.vo.Nickname;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NicknameUniquenessCheckerImpl implements NicknameUniquenessChecker {

    private final UserQueryPort userQueryPort;

    @Override
    public void ensureUnique(Nickname nickname) {
        if (userQueryPort.existsByNickname(nickname.value())) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
    }
}
