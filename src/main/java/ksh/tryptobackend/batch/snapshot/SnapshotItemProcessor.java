package ksh.tryptobackend.batch.snapshot;

import ksh.tryptobackend.ranking.application.port.out.BalanceQueryPort;
import ksh.tryptobackend.ranking.application.port.out.EmergencyFundingSnapshotPort;
import ksh.tryptobackend.ranking.application.port.out.ExchangeInfoQueryPort;
import ksh.tryptobackend.ranking.application.port.out.HoldingQueryPort;
import ksh.tryptobackend.ranking.application.port.out.dto.ExchangeSnapshotInfo;
import ksh.tryptobackend.ranking.domain.model.EvaluatedHoldings;
import ksh.tryptobackend.ranking.domain.model.PortfolioSnapshot;
import ksh.tryptobackend.ranking.domain.model.SnapshotDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@StepScope
@RequiredArgsConstructor
public class SnapshotItemProcessor implements ItemProcessor<SnapshotInput, SnapshotOutput> {

    private final ExchangeInfoQueryPort exchangeInfoQueryPort;
    private final BalanceQueryPort balanceQueryPort;
    private final HoldingQueryPort holdingQueryPort;
    private final EmergencyFundingSnapshotPort emergencyFundingSnapshotPort;

    @Value("#{jobParameters['snapshotDate']}")
    private String snapshotDateParam;

    @Override
    public SnapshotOutput process(SnapshotInput input) {
        LocalDate snapshotDate = LocalDate.parse(snapshotDateParam);
        ExchangeSnapshotInfo exchangeInfo = exchangeInfoQueryPort.getExchangeInfo(input.exchangeId());

        BigDecimal balance = balanceQueryPort.getAvailableBalance(input.walletId(), exchangeInfo.baseCurrencyCoinId());
        EvaluatedHoldings evaluatedHoldings = holdingQueryPort.findAllByWalletId(input.walletId(), input.exchangeId());

        BigDecimal totalAsset = balance.add(evaluatedHoldings.totalEvaluatedAmount());
        BigDecimal totalInvestment = calculateTotalInvestment(input);

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(
            input.userId(), input.roundId(), input.exchangeId(),
            totalAsset, totalInvestment, exchangeInfo.conversionRate(), snapshotDate);

        List<SnapshotDetail> details = evaluatedHoldings.toSnapshotDetails(totalAsset);

        return new SnapshotOutput(snapshot, details);
    }

    private BigDecimal calculateTotalInvestment(SnapshotInput input) {
        BigDecimal emergencyFunding = emergencyFundingSnapshotPort.sumByRoundIdAndExchangeId(
            input.roundId(), input.exchangeId());
        return input.seedAmount().add(emergencyFunding);
    }
}
