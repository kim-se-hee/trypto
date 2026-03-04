package ksh.tryptobackend.ranking.application.service;

import ksh.tryptobackend.ranking.application.port.in.TakePortfolioSnapshotUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.command.TakeSnapshotCommand;
import ksh.tryptobackend.ranking.application.port.in.dto.result.SnapshotResult;
import ksh.tryptobackend.ranking.application.port.out.BalanceQueryPort;
import ksh.tryptobackend.ranking.application.port.out.EmergencyFundingSnapshotPort;
import ksh.tryptobackend.ranking.application.port.out.ExchangeInfoQueryPort;
import ksh.tryptobackend.ranking.application.port.out.HoldingQueryPort;
import ksh.tryptobackend.ranking.application.port.out.dto.ExchangeSnapshotInfo;
import ksh.tryptobackend.ranking.domain.model.EvaluatedHoldings;
import ksh.tryptobackend.ranking.domain.model.PortfolioSnapshot;
import ksh.tryptobackend.ranking.domain.model.SnapshotDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TakePortfolioSnapshotService implements TakePortfolioSnapshotUseCase {

    private final ExchangeInfoQueryPort exchangeInfoQueryPort;
    private final BalanceQueryPort balanceQueryPort;
    private final HoldingQueryPort holdingQueryPort;
    private final EmergencyFundingSnapshotPort emergencyFundingSnapshotPort;

    @Override
    public SnapshotResult takeSnapshot(TakeSnapshotCommand command) {
        ExchangeSnapshotInfo exchangeInfo = exchangeInfoQueryPort.getExchangeInfo(command.exchangeId());
        EvaluatedHoldings evaluatedHoldings = holdingQueryPort.findAllByWalletId(command.walletId(), command.exchangeId());

        BigDecimal totalAsset = calculateTotalAsset(command, exchangeInfo, evaluatedHoldings);
        BigDecimal totalInvestment = calculateTotalInvestment(command);

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(
            command.userId(), command.roundId(), command.exchangeId(),
            totalAsset, totalInvestment, exchangeInfo.conversionRate(), command.snapshotDate());

        List<SnapshotDetail> details = evaluatedHoldings.toSnapshotDetails(totalAsset);

        return new SnapshotResult(snapshot, details);
    }

    private BigDecimal calculateTotalAsset(TakeSnapshotCommand command, ExchangeSnapshotInfo exchangeInfo,
                                           EvaluatedHoldings evaluatedHoldings) {
        BigDecimal balance = balanceQueryPort.getAvailableBalance(command.walletId(), exchangeInfo.baseCurrencyCoinId());
        return balance.add(evaluatedHoldings.totalEvaluatedAmount());
    }

    private BigDecimal calculateTotalInvestment(TakeSnapshotCommand command) {
        BigDecimal emergencyFunding = emergencyFundingSnapshotPort.sumByRoundIdAndExchangeId(
            command.roundId(), command.exchangeId());
        return command.seedAmount().add(emergencyFunding);
    }
}
