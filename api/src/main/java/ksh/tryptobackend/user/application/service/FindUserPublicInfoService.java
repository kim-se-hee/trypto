package ksh.tryptobackend.user.application.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import ksh.tryptobackend.user.application.port.in.FindUserPublicInfoUseCase;
import ksh.tryptobackend.user.application.port.in.dto.result.UserPublicInfoResult;
import ksh.tryptobackend.user.application.port.out.UserQueryPort;
import ksh.tryptobackend.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindUserPublicInfoService implements FindUserPublicInfoUseCase {

    private final UserQueryPort userQueryPort;

    @Override
    public Optional<UserPublicInfoResult> findByUserId(Long userId) {
        return userQueryPort.findById(userId).map(this::toResult);
    }

    @Override
    public List<UserPublicInfoResult> findByUserIds(Set<Long> userIds) {
        return userQueryPort.findByIds(userIds).stream().map(this::toResult).toList();
    }

    private UserPublicInfoResult toResult(User user) {
        return new UserPublicInfoResult(
                user.getUserId(), user.getNickname(), user.isPortfolioPublic());
    }
}
