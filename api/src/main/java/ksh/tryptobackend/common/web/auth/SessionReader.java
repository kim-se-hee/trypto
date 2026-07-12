package ksh.tryptobackend.common.web.auth;

import java.util.Optional;

/**
 * 요청에 실려온 세션 ID 로 로그인 유저를 복원한다. 로그인 시 발급된 세션(쓰기)의 조회 측 관심사로, 인증 인프라가
 * 사용한다.
 */
public interface SessionReader {

    Optional<Long> findUserId(String sessionId);
}
