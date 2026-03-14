package ksh.tryptobackend.marketdata.adapter.in;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.marketdata.adapter.in.dto.request.FindCandlesRequest;
import ksh.tryptobackend.marketdata.adapter.in.dto.response.CandleResponse;
import ksh.tryptobackend.marketdata.application.port.in.FindCandlesUseCase;
import ksh.tryptobackend.marketdata.domain.model.Candle;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/candles")
@RequiredArgsConstructor
public class CandleController {

    private final FindCandlesUseCase findCandlesUseCase;

    @GetMapping
    public ApiResponseDto<List<CandleResponse>> getCandles(@Valid @ModelAttribute FindCandlesRequest request) {
        List<Candle> candles = findCandlesUseCase.findCandles(request.toQuery());
        List<CandleResponse> response = candles.stream()
            .map(CandleResponse::from)
            .toList();
        return ApiResponseDto.success("캔들 데이터를 조회했습니다.", response);
    }
}
