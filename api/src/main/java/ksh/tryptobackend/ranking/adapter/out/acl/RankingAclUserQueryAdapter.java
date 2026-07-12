package ksh.tryptobackend.ranking.adapter.out.acl;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.ranking.application.port.out.UserQueryPort;
import ksh.tryptobackend.ranking.domain.vo.UserProfile;
import ksh.tryptobackend.ranking.domain.vo.UserProfiles;
import ksh.tryptobackend.user.application.port.in.FindUserPublicInfoUseCase;
import ksh.tryptobackend.user.application.port.in.dto.result.UserPublicInfoResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingAclUserQueryAdapter implements UserQueryPort {

    private final FindUserPublicInfoUseCase findUserPublicInfoUseCase;

    @Override
    public UserProfiles findByUserIds(Set<Long> userIds) {
        Map<Long, UserProfile> profileByUserId = findUserPublicInfoUseCase.findByUserIds(userIds).stream()
                .collect(Collectors.toMap(UserPublicInfoResult::userId, this::toProfile));
        return new UserProfiles(profileByUserId);
    }

    @Override
    public UserProfile getByUserId(Long userId) {
        return findUserPublicInfoUseCase
                .findByUserId(userId)
                .map(this::toProfile)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private UserProfile toProfile(UserPublicInfoResult result) {
        return new UserProfile(result.userId(), result.nickname(), result.portfolioPublic());
    }
}
