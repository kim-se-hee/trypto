package ksh.tryptobackend.common.web.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.WebUtils;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTRIBUTE = "authenticatedUserId";
    private static final String SESSION_COOKIE_NAME = "SESSION";

    private final SessionReader sessionReader;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String sessionId = extractSessionId(request);
        if (sessionId == null) {
            throw new CustomException(ErrorCode.UNAUTHENTICATED);
        }

        Long userId =
                sessionReader.findUserId(sessionId).orElseThrow(() -> new CustomException(ErrorCode.UNAUTHENTICATED));

        request.setAttribute(USER_ID_ATTRIBUTE, userId);
        return true;
    }

    private String extractSessionId(HttpServletRequest request) {
        var cookie = WebUtils.getCookie(request, SESSION_COOKIE_NAME);
        return cookie != null ? cookie.getValue() : null;
    }
}
