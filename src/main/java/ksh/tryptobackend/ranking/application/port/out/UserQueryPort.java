package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.application.port.out.dto.UserInfo;

import java.util.Optional;

public interface UserQueryPort {

    Optional<UserInfo> findById(Long userId);
}
