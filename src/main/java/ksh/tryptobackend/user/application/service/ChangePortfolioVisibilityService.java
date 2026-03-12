package ksh.tryptobackend.user.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.user.application.port.in.ChangePortfolioVisibilityUseCase;
import ksh.tryptobackend.user.application.port.in.dto.command.ChangePortfolioVisibilityCommand;
import ksh.tryptobackend.user.application.port.out.UserCommandPort;
import ksh.tryptobackend.user.application.port.out.UserQueryPort;
import ksh.tryptobackend.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangePortfolioVisibilityService implements ChangePortfolioVisibilityUseCase {

    private final UserQueryPort userQueryPort;
    private final UserCommandPort userCommandPort;

    @Override
    @Transactional
    public User changePortfolioVisibility(ChangePortfolioVisibilityCommand command) {
        User user = getUser(command.userId());
        user.changePortfolioVisibility(command.portfolioPublic());
        return userCommandPort.save(user);
    }

    private User getUser(Long userId) {
        return userQueryPort.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
