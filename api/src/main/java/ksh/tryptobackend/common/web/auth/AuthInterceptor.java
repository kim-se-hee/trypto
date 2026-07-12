package ksh.tryptobackend.common.web.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.WebUtils;

/**
 * 보호 대상 요청의 관문. 세션 쿠키를 복원해 userId 를 요청 속성에 담고, 미인증 요청은 401 로 끊는다. 공개 엔드포인트는
 * {@link WebConfig} 의 excludePathPatterns 로 이 인터셉터를 아예 타지 않는다.
 */
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
