package ksh.tryptobackend.marketdata.adapter.in;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.adapter.in.dto.request.FindWithdrawalFeeRequest;
import ksh.tryptobackend.marketdata.adapter.in.dto.response.WithdrawalFeeResponse;
import ksh.tryptobackend.marketdata.application.port.in.FindWithdrawalFeeUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.WithdrawalFeeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/withdrawal-fees")
@RequiredArgsConstructor
public class WithdrawalFeeController {

    private final FindWithdrawalFeeUseCase findWithdrawalFeeUseCase;

    @GetMapping
    public ApiResponseDto<WithdrawalFeeResponse> getWithdrawalFee(
            @Valid @ModelAttribute FindWithdrawalFeeRequest request) {
        WithdrawalFeeResult result = findWithdrawalFeeUseCase
                .findByExchangeIdAndCoinIdAndChain(request.exchangeId(), request.coinId(), request.chain())
                .orElseThrow(() -> new CustomException(ErrorCode.WITHDRAWAL_FEE_NOT_FOUND));
        return ApiResponseDto.success("출금 수수료를 조회했습니다.", WithdrawalFeeResponse.from(result));
    }
}
