package ksh.tryptobackend.user.adapter.in.web;

import ksh.tryptobackend.common.config.SessionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionCookieFactory {

    private static final String SESSION_COOKIE_NAME = "SESSION";
    private static final String SAME_SITE_LAX = "Lax";
    private static final String ROOT_PATH = "/";
    private static final String EMPTY_VALUE = "";
    private static final long EXPIRE_NOW = 0L;

    private final SessionProperties sessionProperties;

    public ResponseCookie issue(String sessionId) {
        return baseCookie(sessionId).maxAge(sessionProperties.getTtl()).build();
    }

    public ResponseCookie expire() {
        return baseCookie(EMPTY_VALUE).maxAge(EXPIRE_NOW).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(SESSION_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(sessionProperties.isSecure())
                .sameSite(SAME_SITE_LAX)
                .path(ROOT_PATH);
    }
}
