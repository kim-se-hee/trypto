package ksh.tryptobackend.portfolio.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.SumEmergencyFundingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.portfolio.application.port.in.TakePortfolioSnapshotUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.command.TakeSnapshotCommand;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotResult;
import ksh.tryptobackend.portfolio.domain.model.EvaluatedHolding;
import ksh.tryptobackend.portfolio.domain.model.EvaluatedHoldings;
import ksh.tryptobackend.portfolio.domain.model.PortfolioSnapshot;
import ksh.tryptobackend.portfolio.domain.model.SnapshotDetail;
import ksh.tryptobackend.portfolio.domain.vo.ExchangeSnapshot;
import ksh.tryptobackend.portfolio.domain.vo.KrwConversionRate;
import ksh.tryptobackend.trading.application.port.in.FindEvaluatedHoldingsUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.EvaluatedHoldingResult;
import ksh.tryptobackend.wallet.application.port.in.GetAvailableBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TakePortfolioSnapshotService implements TakePortfolioSnapshotUseCase {

    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final GetAvailableBalanceUseCase getAvailableBalanceUseCase;
    private final SumEmergencyFundingUseCase sumEmergencyFundingUseCase;
    private final FindEvaluatedHoldingsUseCase findEvaluatedHoldingsUseCase;

    @Override
    public SnapshotResult takeSnapshot(TakeSnapshotCommand command) {
        ExchangeSnapshot exchangeSnapshot = getExchangeSnapshot(command.exchangeId());
        EvaluatedHoldings evaluatedHoldings = buildEvaluatedHoldings(command.walletId(), command.exchangeId());

        BigDecimal totalAsset = calculateTotalAsset(command, exchangeSnapshot, evaluatedHoldings);
        BigDecimal totalInvestment = calculateTotalInvestment(command);

        List<SnapshotDetail> details = evaluatedHoldings.toSnapshotDetails(totalAsset);

        PortfolioSnapshot snapshot = PortfolioSnapshot.create(
            command.userId(), command.roundId(), command.exchangeId(),
            totalAsset, totalInvestment, exchangeSnapshot.conversionRate(), command.snapshotDate(), details);

        return new SnapshotResult(snapshot);
    }

    private ExchangeSnapshot getExchangeSnapshot(Long exchangeId) {
        ExchangeDetailResult detail = findExchangeDetailUseCase.findExchangeDetail(exchangeId)
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
        KrwConversionRate conversionRate = detail.domestic() ? KrwConversionRate.DOMESTIC : KrwConversionRate.OVERSEAS;
        return new ExchangeSnapshot(exchangeId, detail.baseCurrencyCoinId(), conversionRate);
    }

    private EvaluatedHoldings buildEvaluatedHoldings(Long walletId, Long exchangeId) {
        List<EvaluatedHoldingResult> results = findEvaluatedHoldingsUseCase.findEvaluatedHoldings(walletId, exchangeId);

        List<EvaluatedHolding> holdings = results.stream()
            .map(r -> EvaluatedHolding.create(r.coinId(), r.avgBuyPrice(), r.totalQuantity(), r.currentPrice()))
            .toList();

        return new EvaluatedHoldings(holdings);
    }

    private BigDecimal calculateTotalAsset(TakeSnapshotCommand command, ExchangeSnapshot exchangeSnapshot,
                                           EvaluatedHoldings evaluatedHoldings) {
        BigDecimal balance = getAvailableBalanceUseCase.getAvailableBalance(command.walletId(), exchangeSnapshot.baseCurrencyCoinId());
        return balance.add(evaluatedHoldings.totalEvaluatedAmount());
    }

    private BigDecimal calculateTotalInvestment(TakeSnapshotCommand command) {
        BigDecimal emergencyFunding = sumEmergencyFundingUseCase.sumByRoundIdAndExchangeId(
            command.roundId(), command.exchangeId());
        return command.seedAmount().add(emergencyFunding);
    }
}
