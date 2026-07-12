package ksh.tryptobackend.common.web.auth;

import java.util.Optional;

public interface SessionReader {

    Optional<Long> findUserId(String sessionId);
}
