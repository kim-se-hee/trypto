package ksh.tryptobackend.ranking.application.port.out;

import java.util.Optional;
import java.util.Set;
import ksh.tryptobackend.ranking.domain.vo.UserProfile;
import ksh.tryptobackend.ranking.domain.vo.UserProfiles;

public interface UserQueryPort {

    UserProfiles findByUserIds(Set<Long> userIds);

    Optional<UserProfile> findByUserId(Long userId);
}
