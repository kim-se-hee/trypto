package ksh.tryptobackend.user.adapter.in.web;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.common.web.auth.LoginUser;
import ksh.tryptobackend.user.adapter.in.dto.request.SendFeedbackRequest;
import ksh.tryptobackend.user.application.port.in.SendFeedbackUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final SendFeedbackUseCase sendFeedbackUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDto<Void> createFeedback(
            @LoginUser Long userId, @Valid @RequestBody SendFeedbackRequest request) {
        sendFeedbackUseCase.sendFeedback(request.toCommand(userId));
        return ApiResponseDto.createdSuccess("피드백이 접수되었습니다.", null);
    }
}
