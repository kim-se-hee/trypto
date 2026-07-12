package ksh.tryptobackend.portfolio.application.service;

import java.math.BigDecimal;
import java.util.List;
import ksh.tryptobackend.portfolio.application.port.in.TakePortfolioSnapshotUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.command.TakeSnapshotCommand;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotResult;
import ksh.tryptobackend.portfolio.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.portfolio.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.portfolio.application.port.out.TradingQueryPort;
import ksh.tryptobackend.portfolio.application.port.out.WalletQueryPort;
import ksh.tryptobackend.portfolio.domain.model.EvaluatedHoldings;
import ksh.tryptobackend.portfolio.domain.model.PortfolioSnapshot;
import ksh.tryptobackend.portfolio.domain.model.SnapshotDetail;
import ksh.tryptobackend.portfolio.domain.vo.ExchangeSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TakePortfolioSnapshotService implements TakePortfolioSnapshotUseCase {

    private final MarketDataQueryPort marketDataQueryPort;
    private final WalletQueryPort walletQueryPort;
    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final TradingQueryPort tradingQueryPort;

    @Override
    public SnapshotResult takeSnapshot(TakeSnapshotCommand command) {
        ExchangeSnapshot exchangeSnapshot = marketDataQueryPort.getExchangeSnapshot(command.exchangeId());
        EvaluatedHoldings evaluatedHoldings =
                tradingQueryPort.findEvaluatedHoldings(command.walletId(), command.exchangeId());

        BigDecimal totalAsset = calculateTotalAsset(command, exchangeSnapshot, evaluatedHoldings);
        BigDecimal totalInvestment = calculateTotalInvestment(command);

        List<SnapshotDetail> details = evaluatedHoldings.toSnapshotDetails(totalAsset);

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(
                command.userId(),
                command.roundId(),
                command.exchangeId(),
                totalAsset,
                totalInvestment,
                exchangeSnapshot.conversionRate(),
                command.snapshotDate(),
                details);

        return new SnapshotResult(snapshot);
    }

    private BigDecimal calculateTotalAsset(
            TakeSnapshotCommand command, ExchangeSnapshot exchangeSnapshot, EvaluatedHoldings evaluatedHoldings) {
        BigDecimal balance =
                walletQueryPort.getAvailableBalance(command.walletId(), exchangeSnapshot.baseCurrencyCoinId());
        return balance.add(evaluatedHoldings.totalEvaluatedAmount());
    }

    private BigDecimal calculateTotalInvestment(TakeSnapshotCommand command) {
        BigDecimal emergencyFunding =
                investmentRoundQueryPort.sumEmergencyFunding(command.roundId(), command.exchangeId());
        return command.seedAmount().add(emergencyFunding);
    }
}
