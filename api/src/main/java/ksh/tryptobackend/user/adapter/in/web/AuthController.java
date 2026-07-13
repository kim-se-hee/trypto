package ksh.tryptobackend.user.adapter.in.web;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.user.adapter.in.dto.request.LoginRequest;
import ksh.tryptobackend.user.adapter.in.dto.response.LoginResponse;
import ksh.tryptobackend.user.application.port.in.LoginUseCase;
import ksh.tryptobackend.user.application.port.in.LogoutUseCase;
import ksh.tryptobackend.user.application.port.in.dto.result.LoginResult;
import ksh.tryptobackend.user.domain.vo.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String SESSION_COOKIE_NAME = "SESSION";

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final SessionCookieFactory sessionCookieFactory;

    @PostMapping("/{provider}/login")
    public ResponseEntity<ApiResponseDto<LoginResponse>> login(
            @PathVariable String provider, @Valid @RequestBody LoginRequest request) {
        LoginResult result = loginUseCase.login(request.toCommand(Provider.from(provider)));
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        sessionCookieFactory.issue(result.sessionId()).toString())
                .body(ApiResponseDto.success("로그인되었습니다.", LoginResponse.from(result)));
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
