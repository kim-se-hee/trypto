package ksh.tryptobackend.portfolio.adapter.out.acl;

import java.util.List;
import ksh.tryptobackend.portfolio.application.port.out.TradingQueryPort;
import ksh.tryptobackend.portfolio.domain.model.EvaluatedHolding;
import ksh.tryptobackend.portfolio.domain.model.EvaluatedHoldings;
import ksh.tryptobackend.portfolio.domain.vo.PortfolioHolding;
import ksh.tryptobackend.portfolio.domain.vo.PortfolioHoldings;
import ksh.tryptobackend.trading.application.port.in.FindEvaluatedHoldingsUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.EvaluatedHoldingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortfolioAclTradingQueryAdapter implements TradingQueryPort {

    private final FindEvaluatedHoldingsUseCase findEvaluatedHoldingsUseCase;

    @Override
    public PortfolioHoldings findHoldings(Long walletId, Long exchangeId) {
        List<PortfolioHolding> holdings =
                findEvaluatedHoldingsUseCase.findEvaluatedHoldings(walletId, exchangeId).stream()
                        .map(result -> new PortfolioHolding(
                                result.coinId(), result.avgBuyPrice(), result.totalQuantity(), result.currentPrice()))
                        .toList();
        return new PortfolioHoldings(holdings);
    }

    @Override
    public EvaluatedHoldings findEvaluatedHoldings(Long walletId, Long exchangeId) {
        List<EvaluatedHolding> holdings =
                findEvaluatedHoldingsUseCase.findEvaluatedHoldings(walletId, exchangeId).stream()
                        .map(this::toEvaluatedHolding)
                        .toList();
        return new EvaluatedHoldings(holdings);
    }

    private EvaluatedHolding toEvaluatedHolding(EvaluatedHoldingResult result) {
        return EvaluatedHolding.create(
                result.coinId(), result.avgBuyPrice(), result.totalQuantity(), result.currentPrice());
    }
}
