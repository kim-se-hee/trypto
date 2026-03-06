package ksh.tryptobackend.ranking.application.service;

import ksh.tryptobackend.ranking.application.port.in.TakePortfolioSnapshotUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.command.TakeSnapshotCommand;
import ksh.tryptobackend.ranking.application.port.in.dto.result.SnapshotResult;
import ksh.tryptobackend.ranking.application.port.out.BalanceQueryPort;
import ksh.tryptobackend.ranking.application.port.out.EmergencyFundingSnapshotPort;
import ksh.tryptobackend.ranking.application.port.out.ExchangeSnapshotPort;
import ksh.tryptobackend.ranking.application.port.out.EvaluatedHoldingQueryPort;
import ksh.tryptobackend.ranking.domain.model.EvaluatedHoldings;
import ksh.tryptobackend.ranking.domain.model.PortfolioSnapshot;
import ksh.tryptobackend.ranking.domain.model.SnapshotDetail;
import ksh.tryptobackend.ranking.domain.vo.ExchangeSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TakePortfolioSnapshotService implements TakePortfolioSnapshotUseCase {

    private final ExchangeSnapshotPort exchangeSnapshotPort;
    private final BalanceQueryPort balanceQueryPort;
    private final EvaluatedHoldingQueryPort evaluatedHoldingQueryPort;
    private final EmergencyFundingSnapshotPort emergencyFundingSnapshotPort;

    @Override
    public SnapshotResult takeSnapshot(TakeSnapshotCommand command) {
        ExchangeSnapshot exchangeSnapshot = exchangeSnapshotPort.getExchangeInfo(command.exchangeId());
        EvaluatedHoldings evaluatedHoldings = evaluatedHoldingQueryPort.findAllByWalletId(command.walletId(), command.exchangeId());

        BigDecimal totalAsset = calculateTotalAsset(command, exchangeSnapshot, evaluatedHoldings);
        BigDecimal totalInvestment = calculateTotalInvestment(command);

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(
            command.userId(), command.roundId(), command.exchangeId(),
            totalAsset, totalInvestment, exchangeSnapshot.conversionRate(), command.snapshotDate());

        List<SnapshotDetail> details = evaluatedHoldings.toSnapshotDetails(totalAsset);

        return new SnapshotResult(snapshot, details);
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
