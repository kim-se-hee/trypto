package ksh.tryptobackend.portfolio.adapter.out.acl;

import java.util.List;
import ksh.tryptobackend.portfolio.application.port.out.TradingQueryPort;
import ksh.tryptobackend.portfolio.domain.vo.PortfolioHolding;
import ksh.tryptobackend.portfolio.domain.vo.PortfolioHoldings;
import ksh.tryptobackend.trading.application.port.in.FindEvaluatedHoldingsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AclTradingQueryAdapter implements TradingQueryPort {

    private final FindEvaluatedHoldingsUseCase findEvaluatedHoldingsUseCase;

    @Override
    public PortfolioHoldings findHoldings(Long walletId, Long exchangeId) {
        List<PortfolioHolding> holdings =
                findEvaluatedHoldingsUseCase.findEvaluatedHoldings(walletId, exchangeId).stream()
                        .map(
                                result ->
                                        new PortfolioHolding(
                                                result.coinId(),
                                                result.avgBuyPrice(),
                                                result.totalQuantity(),
                                                result.currentPrice()))
                        .toList();
        return new PortfolioHoldings(holdings);
    }
}
