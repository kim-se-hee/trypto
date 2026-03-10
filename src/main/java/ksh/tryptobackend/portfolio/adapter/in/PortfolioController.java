package ksh.tryptobackend.portfolio.adapter.in;

import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.portfolio.adapter.in.dto.response.MyHoldingsResponse;
import ksh.tryptobackend.portfolio.application.port.in.GetMyHoldingsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.query.GetMyHoldingsQuery;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.MyHoldingsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/wallets/{walletId}/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final GetMyHoldingsUseCase getMyHoldingsUseCase;

    @GetMapping
    public ApiResponseDto<MyHoldingsResponse> getMyHoldings(
            @PathVariable Long userId,
            @PathVariable Long walletId) {
        MyHoldingsResult result = getMyHoldingsUseCase.getMyHoldings(new GetMyHoldingsQuery(userId, walletId));
        return ApiResponseDto.success("포트폴리오를 조회했습니다.", MyHoldingsResponse.from(result));
    }
}
