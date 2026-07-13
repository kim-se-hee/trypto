package ksh.tryptobackend.user.adapter.in.web;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.common.web.auth.LoginUser;
import ksh.tryptobackend.user.adapter.in.dto.request.ChangeNicknameRequest;
import ksh.tryptobackend.user.adapter.in.dto.request.ChangePortfolioVisibilityRequest;
import ksh.tryptobackend.user.adapter.in.dto.response.ChangeNicknameResponse;
import ksh.tryptobackend.user.adapter.in.dto.response.ChangePortfolioVisibilityResponse;
import ksh.tryptobackend.user.adapter.in.dto.response.UserProfileResponse;
import ksh.tryptobackend.user.application.port.in.ChangeNicknameUseCase;
import ksh.tryptobackend.user.application.port.in.ChangePortfolioVisibilityUseCase;
import ksh.tryptobackend.user.application.port.in.DeleteAccountUseCase;
import ksh.tryptobackend.user.application.port.in.GetUserProfileUseCase;
import ksh.tryptobackend.user.application.port.in.dto.command.DeleteAccountCommand;
import ksh.tryptobackend.user.application.port.in.dto.query.GetUserProfileQuery;
import ksh.tryptobackend.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final GetUserProfileUseCase getUserProfileUseCase;
    private final ChangeNicknameUseCase changeNicknameUseCase;
    private final ChangePortfolioVisibilityUseCase changePortfolioVisibilityUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;
    private final SessionCookieFactory sessionCookieFactory;

    @GetMapping("/me")
    public ApiResponseDto<UserProfileResponse> getUserProfile(@LoginUser Long userId) {
        User user = getUserProfileUseCase.getUserProfile(new GetUserProfileQuery(userId));
        return ApiResponseDto.success("사용자 프로필을 조회했습니다.", UserProfileResponse.from(user));
    }

    @PutMapping("/me/nickname")
    public ApiResponseDto<ChangeNicknameResponse> changeNickname(
            @LoginUser Long userId, @Valid @RequestBody ChangeNicknameRequest request) {
        User user = changeNicknameUseCase.changeNickname(request.toCommand(userId));
        return ApiResponseDto.success("닉네임이 변경되었습니다.", ChangeNicknameResponse.from(user));
    }

    @PutMapping("/me/portfolio-visibility")
    public ApiResponseDto<ChangePortfolioVisibilityResponse> changePortfolioVisibility(
            @LoginUser Long userId, @Valid @RequestBody ChangePortfolioVisibilityRequest request) {
        User user = changePortfolioVisibilityUseCase.changePortfolioVisibility(request.toCommand(userId));
        return ApiResponseDto.success("포트폴리오 공개 설정이 변경되었습니다.", ChangePortfolioVisibilityResponse.from(user));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponseDto<Void>> deleteAccount(@LoginUser Long userId) {
        deleteAccountUseCase.deleteAccount(new DeleteAccountCommand(userId));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookieFactory.expire().toString())
                .body(ApiResponseDto.success("회원 탈퇴가 완료되었습니다.", null));
    }
}
