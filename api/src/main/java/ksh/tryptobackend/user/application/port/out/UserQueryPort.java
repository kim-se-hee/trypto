package ksh.tryptobackend.user.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import ksh.tryptobackend.user.domain.model.User;

public interface UserQueryPort {

    Optional<User> findById(Long userId);

    List<User> findByIds(Set<Long> userIds);

    Optional<User> findLatestWithdrawnBySocialAccountId(Long socialAccountId);

    boolean existsByNickname(String nickname);
}
