package ksh.tryptobackend.ranking.application.port.in.dto.result;

import java.math.BigDecimal;
import ksh.tryptobackend.ranking.domain.vo.HoldingView;

public record PortfolioHoldingResult(
        String coinSymbol, String exchangeName, BigDecimal assetRatio, BigDecimal profitRate) {

    public static PortfolioHoldingResult from(HoldingView view) {
        return new PortfolioHoldingResult(
                view.coinSymbol(), view.exchangeName(), view.assetRatio(), view.profitRate());
    }
}
