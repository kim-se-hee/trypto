package ksh.tryptobackend.user.adapter.in.web;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.user.adapter.in.dto.request.KakaoLoginRequest;
import ksh.tryptobackend.user.adapter.in.dto.response.KakaoLoginResponse;
import ksh.tryptobackend.user.application.port.in.KakaoLoginUseCase;
import ksh.tryptobackend.user.application.port.in.LogoutUseCase;
import ksh.tryptobackend.user.application.port.in.dto.result.KakaoLoginResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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

    private final KakaoLoginUseCase kakaoLoginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final SessionCookieFactory sessionCookieFactory;

    @PostMapping("/kakao/login")
    public ResponseEntity<ApiResponseDto<KakaoLoginResponse>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request) {
        KakaoLoginResult result = kakaoLoginUseCase.login(request.toCommand());
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        sessionCookieFactory.issue(result.sessionId()).toString())
                .body(ApiResponseDto.success("로그인되었습니다.", KakaoLoginResponse.from(result)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<Void>> logout(
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String sessionId) {
        if (sessionId != null) {
            logoutUseCase.logout(sessionId);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookieFactory.expire().toString())
                .body(ApiResponseDto.success("로그아웃되었습니다.", null));
    }
}
