package ksh.tryptobackend.user.adapter.in.web;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.config.SessionProperties;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.user.adapter.in.dto.request.KakaoLoginRequest;
import ksh.tryptobackend.user.adapter.in.dto.response.KakaoLoginResponse;
import ksh.tryptobackend.user.application.port.in.KakaoLoginUseCase;
import ksh.tryptobackend.user.application.port.in.LogoutUseCase;
import ksh.tryptobackend.user.application.port.in.dto.result.KakaoLoginResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String SESSION_COOKIE_NAME = "SESSION";
    private static final String SAME_SITE_LAX = "Lax";
    private static final String ROOT_PATH = "/";

    private final KakaoLoginUseCase kakaoLoginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final SessionProperties sessionProperties;

    @PostMapping("/kakao/login")
    public ResponseEntity<ApiResponseDto<KakaoLoginResponse>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request) {
        KakaoLoginResult result = kakaoLoginUseCase.login(request.toCommand());
        ResponseCookie sessionCookie = buildSessionCookie(result.sessionId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .body(ApiResponseDto.success("로그인되었습니다.", KakaoLoginResponse.from(result)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<Void>> logout(
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId) {
        if (sessionId != null) {
            logoutUseCase.logout(sessionId);
        }
        ResponseCookie expiredCookie = buildExpiredSessionCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .body(ApiResponseDto.success("로그아웃되었습니다.", null));
    }

    private ResponseCookie buildSessionCookie(String sessionId) {
        return ResponseCookie.from(SESSION_COOKIE_NAME, sessionId)
                .httpOnly(true)
                .secure(sessionProperties.isSecure())
                .sameSite(SAME_SITE_LAX)
                .path(ROOT_PATH)
                .maxAge(sessionProperties.getTtl())
                .build();
    }

    private ResponseCookie buildExpiredSessionCookie() {
        return ResponseCookie.from(SESSION_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(sessionProperties.isSecure())
                .sameSite(SAME_SITE_LAX)
                .path(ROOT_PATH)
                .maxAge(0)
                .build();
    }
}
