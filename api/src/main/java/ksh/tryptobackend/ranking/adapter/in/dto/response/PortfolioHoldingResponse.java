package ksh.tryptobackend.ranking.adapter.in.dto.response;

import java.math.BigDecimal;
import ksh.tryptobackend.ranking.application.port.in.dto.result.PortfolioHoldingResult;

public record PortfolioHoldingResponse(
        String coinSymbol, String exchangeName, BigDecimal assetRatio, BigDecimal profitRate) {

    public static PortfolioHoldingResponse from(PortfolioHoldingResult result) {
        return new PortfolioHoldingResponse(
                result.coinSymbol(), result.exchangeName(), result.assetRatio(), result.profitRate());
    }
}
