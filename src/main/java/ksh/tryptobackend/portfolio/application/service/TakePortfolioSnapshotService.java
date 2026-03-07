package ksh.tryptobackend.portfolio.application.service;

import ksh.tryptobackend.portfolio.application.port.in.TakePortfolioSnapshotUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.command.TakeSnapshotCommand;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotResult;
import ksh.tryptobackend.portfolio.application.port.out.BalanceQueryPort;
import ksh.tryptobackend.portfolio.application.port.out.EmergencyFundingSnapshotQueryPort;
import ksh.tryptobackend.portfolio.application.port.out.ExchangeSnapshotQueryPort;
import ksh.tryptobackend.portfolio.application.port.out.EvaluatedHoldingQueryPort;
import ksh.tryptobackend.portfolio.domain.model.EvaluatedHoldings;
import ksh.tryptobackend.portfolio.domain.model.PortfolioSnapshot;
import ksh.tryptobackend.portfolio.domain.model.SnapshotDetail;
import ksh.tryptobackend.portfolio.domain.vo.ExchangeSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TakePortfolioSnapshotService implements TakePortfolioSnapshotUseCase {

    private final ExchangeSnapshotQueryPort exchangeSnapshotPort;
    private final BalanceQueryPort balanceQueryPort;
    private final EvaluatedHoldingQueryPort evaluatedHoldingQueryPort;
    private final EmergencyFundingSnapshotQueryPort emergencyFundingSnapshotPort;

    @Override
    public SnapshotResult takeSnapshot(TakeSnapshotCommand command) {
        ExchangeSnapshot exchangeSnapshot = exchangeSnapshotPort.getExchangeInfo(command.exchangeId());
        EvaluatedHoldings evaluatedHoldings = evaluatedHoldingQueryPort.findAllByWalletId(command.walletId(), command.exchangeId());

        BigDecimal totalAsset = calculateTotalAsset(command, exchangeSnapshot, evaluatedHoldings);
        BigDecimal totalInvestment = calculateTotalInvestment(command);

        List<SnapshotDetail> details = evaluatedHoldings.toSnapshotDetails(totalAsset);

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(
            command.userId(), command.roundId(), command.exchangeId(),
            totalAsset, totalInvestment, exchangeSnapshot.conversionRate(), command.snapshotDate(), details);

        return new SnapshotResult(snapshot);
    }

    private BigDecimal calculateTotalAsset(TakeSnapshotCommand command, ExchangeSnapshot exchangeSnapshot,
                                           EvaluatedHoldings evaluatedHoldings) {
        BigDecimal balance = balanceQueryPort.getAvailableBalance(command.walletId(), exchangeSnapshot.baseCurrencyCoinId());
        return balance.add(evaluatedHoldings.totalEvaluatedAmount());
    }

    private BigDecimal calculateTotalInvestment(TakeSnapshotCommand command) {
        BigDecimal emergencyFunding = emergencyFundingSnapshotPort.sumByRoundIdAndExchangeId(
            command.roundId(), command.exchangeId());
        return command.seedAmount().add(emergencyFunding);
    }
}
