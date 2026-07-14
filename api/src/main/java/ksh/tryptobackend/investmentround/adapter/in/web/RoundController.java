package ksh.tryptobackend.investmentround.adapter.in.web;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.DuplicateRequestException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.common.web.auth.LoginUser;
import ksh.tryptobackend.investmentround.adapter.in.dto.request.ChargeEmergencyFundingRequest;
import ksh.tryptobackend.investmentround.adapter.in.dto.request.StartRoundRequest;
import ksh.tryptobackend.investmentround.adapter.in.dto.response.ChargeEmergencyFundingResponse;
import ksh.tryptobackend.investmentround.adapter.in.dto.response.EndRoundResponse;
import ksh.tryptobackend.investmentround.adapter.in.dto.response.GetActiveRoundResponse;
import ksh.tryptobackend.investmentround.adapter.in.dto.response.RoundSummaryResponse;
import ksh.tryptobackend.investmentround.adapter.in.dto.response.StartRoundResponse;
import ksh.tryptobackend.investmentround.application.port.in.ChargeEmergencyFundingUseCase;
import ksh.tryptobackend.investmentround.application.port.in.EndRoundUseCase;
import ksh.tryptobackend.investmentround.application.port.in.FindRoundInfoUseCase;
import ksh.tryptobackend.investmentround.application.port.in.GetActiveRoundUseCase;
import ksh.tryptobackend.investmentround.application.port.in.GetRoundSummaryUseCase;
import ksh.tryptobackend.investmentround.application.port.in.StartRoundUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.command.EndRoundCommand;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.GetActiveRoundQuery;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.GetRoundSummaryQuery;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.GetActiveRoundResult;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RoundInfoResult;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RoundSummaryResult;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.StartRoundResult;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rounds")
@RequiredArgsConstructor
public class RoundController {

    private final StartRoundUseCase startRoundUseCase;
    private final EndRoundUseCase endRoundUseCase;
    private final GetActiveRoundUseCase getActiveRoundUseCase;
    private final GetRoundSummaryUseCase getRoundSummaryUseCase;
    private final ChargeEmergencyFundingUseCase chargeEmergencyFundingUseCase;
    private final FindRoundInfoUseCase findRoundInfoUseCase;

    @PostMapping
    public ResponseEntity<ApiResponseDto<StartRoundResponse>> createRound(
            @LoginUser Long userId, @Valid @RequestBody StartRoundRequest request) {
        StartRoundResult result = startRoundUseCase.startRound(request.toCommand(userId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.created("투자 라운드가 시작되었습니다.", StartRoundResponse.from(result)));
    }

    @PostMapping("/{roundId}/end")
    public ResponseEntity<ApiResponseDto<EndRoundResponse>> endRound(
            @PathVariable Long roundId, @LoginUser Long userId) {
        InvestmentRound round = endRoundUseCase.endRound(new EndRoundCommand(roundId, userId));
        return ResponseEntity.ok(ApiResponseDto.success("라운드를 종료했습니다.", EndRoundResponse.from(round)));
    }

    @GetMapping("/active")
    public ApiResponseDto<GetActiveRoundResponse> getActiveRound(@LoginUser Long userId) {
        GetActiveRoundResult result = getActiveRoundUseCase.getActiveRound(new GetActiveRoundQuery(userId));
        return ApiResponseDto.success("활성 라운드를 조회했습니다.", GetActiveRoundResponse.from(result));
    }

    @GetMapping("/summary")
    public ApiResponseDto<RoundSummaryResponse> getRoundSummary(@LoginUser Long userId) {
        RoundSummaryResult result = getRoundSummaryUseCase.getRoundSummary(new GetRoundSummaryQuery(userId));
        return ApiResponseDto.success("라운드 요약을 조회했습니다.", RoundSummaryResponse.from(result));
    }

    @PostMapping("/{roundId}/emergency-funding")
    public ResponseEntity<ApiResponseDto<ChargeEmergencyFundingResponse>> chargeEmergencyFunding(
            @PathVariable Long roundId,
            @LoginUser Long userId,
            @Valid @RequestBody ChargeEmergencyFundingRequest request) {
        int remainingChargeCount;
        try {
            InvestmentRound round = chargeEmergencyFundingUseCase.charge(request.toCommand(roundId, userId));
            remainingChargeCount = round.getEmergencyChargeCount();
        } catch (DuplicateRequestException e) {
            remainingChargeCount = findRoundInfoUseCase
                    .findById(roundId)
                    .map(RoundInfoResult::emergencyChargeCount)
                    .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_FOUND));
        }

        ChargeEmergencyFundingResponse response =
                ChargeEmergencyFundingResponse.of(roundId, request.amount(), remainingChargeCount);
        return ResponseEntity.ok(ApiResponseDto.success("긴급 자금을 투입했습니다.", response));
    }
}
