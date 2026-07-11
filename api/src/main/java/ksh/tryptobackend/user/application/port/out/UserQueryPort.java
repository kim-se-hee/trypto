package ksh.tryptobackend.user.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import ksh.tryptobackend.user.domain.model.User;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;

public interface UserQueryPort {

    Optional<User> findById(Long userId);

    List<User> findByIds(Set<Long> userIds);

    Optional<User> findBySocialIdentity(SocialIdentity socialIdentity);

    boolean existsByNickname(String nickname);
}
