package ksh.tryptobackend.marketdata.adapter.in;

import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.marketdata.adapter.in.dto.response.CoinChainResponse;
import ksh.tryptobackend.marketdata.application.port.in.FindCoinChainsUseCase;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoinChain;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exchanges/{exchangeId}/coins/{coinId}/chains")
@RequiredArgsConstructor
public class ExchangeCoinChainController {

    private final FindCoinChainsUseCase findCoinChainsUseCase;

    @GetMapping
    public ApiResponseDto<List<CoinChainResponse>> getCoinChains(
            @PathVariable Long exchangeId,
            @PathVariable Long coinId) {
        List<ExchangeCoinChain> chains = findCoinChainsUseCase.findByExchangeIdAndCoinId(exchangeId, coinId);
        List<CoinChainResponse> response = chains.stream()
                .map(CoinChainResponse::from)
                .toList();
        return ApiResponseDto.success("코인 체인 목록을 조회했습니다.", response);
    }
}
