package ksh.tryptobackend.user.adapter.in;

import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.user.adapter.in.dto.response.UserProfileResponse;
import ksh.tryptobackend.user.application.port.in.GetUserProfileUseCase;
import ksh.tryptobackend.user.application.port.in.dto.query.GetUserProfileQuery;
import ksh.tryptobackend.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final GetUserProfileUseCase getUserProfileUseCase;

    @GetMapping("/{userId}")
    public ApiResponseDto<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        User user = getUserProfileUseCase.getUserProfile(new GetUserProfileQuery(userId));
        return ApiResponseDto.success("사용자 프로필을 조회했습니다.", UserProfileResponse.from(user));
    }
}
