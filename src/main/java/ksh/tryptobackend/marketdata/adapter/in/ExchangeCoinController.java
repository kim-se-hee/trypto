package ksh.tryptobackend.marketdata.adapter.in;

import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.marketdata.adapter.in.dto.response.ExchangeCoinResponse;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinListResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exchanges")
@RequiredArgsConstructor
public class ExchangeCoinController {

    private final FindExchangeCoinsUseCase findExchangeCoinsUseCase;

    @GetMapping("/{exchangeId}/coins")
    public ApiResponseDto<List<ExchangeCoinResponse>> getExchangeCoins(@PathVariable Long exchangeId) {
        List<ExchangeCoinListResult> results = findExchangeCoinsUseCase.findByExchangeId(exchangeId);
        List<ExchangeCoinResponse> response = results.stream()
            .map(ExchangeCoinResponse::from)
            .toList();
        return ApiResponseDto.success("거래소 상장 코인 목록을 조회했습니다.", response);
    }
}
