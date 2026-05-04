package ksh.tryptobackend.user.application.port.in;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import ksh.tryptobackend.user.application.port.in.dto.result.UserPublicInfoResult;

public interface FindUserPublicInfoUseCase {

    Optional<UserPublicInfoResult> findByUserId(Long userId);

    List<UserPublicInfoResult> findByUserIds(Set<Long> userIds);
}
