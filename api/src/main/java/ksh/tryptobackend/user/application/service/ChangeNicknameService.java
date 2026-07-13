package ksh.tryptobackend.user.application.service;

import ksh.tryptobackend.user.application.port.in.ChangeNicknameUseCase;
import ksh.tryptobackend.user.application.port.in.dto.command.ChangeNicknameCommand;
import ksh.tryptobackend.user.application.port.out.UserCommandPort;
import ksh.tryptobackend.user.application.port.out.UserQueryPort;
import ksh.tryptobackend.user.domain.model.User;
import ksh.tryptobackend.user.domain.service.NicknameUniquenessChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeNicknameService implements ChangeNicknameUseCase {

    private final UserQueryPort userQueryPort;
    private final UserCommandPort userCommandPort;
    private final NicknameUniquenessChecker nicknameUniquenessChecker;

    @Override
    @Transactional
    public User changeNickname(ChangeNicknameCommand command) {
        User user = userQueryPort.getById(command.userId());
        user.changeNickname(command.nickname());
        nicknameUniquenessChecker.ensureUnique(user.getNickname());
        return userCommandPort.save(user);
    }
}
