package ksh.tryptobackend.ranking.adapter.in.dto.response;

import ksh.tryptobackend.ranking.application.port.in.dto.result.RankerPortfolioResult;

import java.math.BigDecimal;
import java.util.List;

public record RankerPortfolioResponse(
    Long userId,
    String nickname,
    int rank,
    BigDecimal profitRate,
    List<PortfolioHoldingResponse> holdings
) {

    public static RankerPortfolioResponse from(RankerPortfolioResult result) {
        return new RankerPortfolioResponse(
            result.userId(),
            result.nickname(),
            result.rank(),
            result.profitRate(),
            result.holdings().stream()
                .map(PortfolioHoldingResponse::from)
                .toList()
        );
    }
}
