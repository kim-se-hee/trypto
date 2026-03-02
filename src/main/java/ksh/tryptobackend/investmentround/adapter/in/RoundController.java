package ksh.tryptobackend.investmentround.adapter.in;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.investmentround.adapter.in.dto.request.ChargeEmergencyFundingRequest;
import ksh.tryptobackend.investmentround.adapter.in.dto.request.EndRoundRequest;
import ksh.tryptobackend.investmentround.adapter.in.dto.request.GetActiveRoundRequest;
import ksh.tryptobackend.investmentround.adapter.in.dto.request.StartRoundRequest;
import ksh.tryptobackend.investmentround.adapter.in.dto.response.ChargeEmergencyFundingResponse;
import ksh.tryptobackend.investmentround.adapter.in.dto.response.EndRoundResponse;
import ksh.tryptobackend.investmentround.adapter.in.dto.response.GetActiveRoundResponse;
import ksh.tryptobackend.investmentround.adapter.in.dto.response.StartRoundResponse;
import ksh.tryptobackend.investmentround.application.port.in.ChargeEmergencyFundingUseCase;
import ksh.tryptobackend.investmentround.application.port.in.EndRoundUseCase;
import ksh.tryptobackend.investmentround.application.port.in.GetActiveRoundUseCase;
import ksh.tryptobackend.investmentround.application.port.in.StartRoundUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.ChargeEmergencyFundingResult;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.GetActiveRoundResult;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.StartRoundResult;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
    private final ChargeEmergencyFundingUseCase chargeEmergencyFundingUseCase;

    @PostMapping
    public ResponseEntity<ApiResponseDto<StartRoundResponse>> createRound(
        @Valid @RequestBody StartRoundRequest request
    ) {
        StartRoundResult result = startRoundUseCase.startRound(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponseDto.created("투자 라운드가 시작되었습니다.", StartRoundResponse.from(result)));
    }

    @PostMapping("/{roundId}/end")
    public ResponseEntity<ApiResponseDto<EndRoundResponse>> endRound(@PathVariable Long roundId,
                                                                     @Valid @RequestBody EndRoundRequest request) {
        InvestmentRound round = endRoundUseCase.endRound(request.toCommand(roundId));
        return ResponseEntity.ok(
            ApiResponseDto.success("라운드를 종료했습니다.", EndRoundResponse.from(round)));
    }

    @GetMapping("/active")
    public ApiResponseDto<GetActiveRoundResponse> getActiveRound(@Valid @ModelAttribute GetActiveRoundRequest request) {
        GetActiveRoundResult result = getActiveRoundUseCase.getActiveRound(request.toQuery());
        return ApiResponseDto.success("활성 라운드를 조회했습니다.", GetActiveRoundResponse.from(result));
    }

    @PostMapping("/{roundId}/emergency-funding")
    public ResponseEntity<ApiResponseDto<ChargeEmergencyFundingResponse>> chargeEmergencyFunding(
        @PathVariable Long roundId,
        @Valid @RequestBody ChargeEmergencyFundingRequest request
    ) {
        ChargeEmergencyFundingResult result = chargeEmergencyFundingUseCase
            .chargeEmergencyFunding(request.toCommand(roundId));
        return ResponseEntity.ok(
            ApiResponseDto.success("긴급 자금을 투입했습니다.", ChargeEmergencyFundingResponse.from(result)));
    }
}
