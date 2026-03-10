package ksh.tryptobackend.portfolio.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.SumEmergencyFundingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
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
import ksh.tryptobackend.trading.application.port.in.FindActiveHoldingsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.GetLivePriceUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.HoldingInfoResult;
import ksh.tryptobackend.wallet.application.port.in.GetAvailableBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TakePortfolioSnapshotService implements TakePortfolioSnapshotUseCase {

    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final GetAvailableBalanceUseCase getAvailableBalanceUseCase;
    private final SumEmergencyFundingUseCase sumEmergencyFundingUseCase;
    private final FindActiveHoldingsUseCase findActiveHoldingsUseCase;
    private final FindExchangeCoinMappingUseCase findExchangeCoinMappingUseCase;
    private final GetLivePriceUseCase getLivePriceUseCase;

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
        List<HoldingInfoResult> holdings = findActiveHoldingsUseCase.findActiveHoldings(walletId);
        if (holdings.isEmpty()) {
            return new EvaluatedHoldings(List.of());
        }

        List<Long> coinIds = holdings.stream().map(HoldingInfoResult::coinId).toList();
        Map<Long, Long> exchangeCoinIdMap = findExchangeCoinMappingUseCase.findExchangeCoinIdMap(exchangeId, coinIds);

        List<EvaluatedHolding> evaluatedHoldings = holdings.stream()
            .map(holding -> {
                Long exchangeCoinId = exchangeCoinIdMap.get(holding.coinId());
                BigDecimal currentPrice = getLivePriceUseCase.getCurrentPrice(exchangeCoinId);
                return EvaluatedHolding.create(holding.coinId(), holding.avgBuyPrice(), holding.totalQuantity(), currentPrice);
            })
            .toList();

        return new EvaluatedHoldings(evaluatedHoldings);
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
