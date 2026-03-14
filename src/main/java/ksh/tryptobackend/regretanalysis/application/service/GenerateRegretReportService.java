package ksh.tryptobackend.regretanalysis.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotInfoResult;
import ksh.tryptobackend.regretanalysis.application.port.in.GenerateRegretReportUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.command.GenerateRegretReportCommand;
import ksh.tryptobackend.regretanalysis.domain.model.AssetSnapshot;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.RuleImpact;
import ksh.tryptobackend.regretanalysis.domain.model.ViolatedOrder;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetails;
import ksh.tryptobackend.regretanalysis.domain.vo.CurrentPrices;
import ksh.tryptobackend.regretanalysis.domain.vo.TradeSide;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLossContext.SoldPortion;
import ksh.tryptobackend.trading.application.port.in.FindViolatedOrdersUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.query.FindViolatedOrdersQuery;
import ksh.tryptobackend.trading.application.port.in.dto.result.SoldPortionResult;
import ksh.tryptobackend.trading.application.port.in.dto.result.ViolatedOrderResult;
import ksh.tryptobackend.marketdata.application.port.in.GetLivePriceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GenerateRegretReportService implements GenerateRegretReportUseCase {

    private final FindViolatedOrdersUseCase findViolatedOrdersUseCase;
    private final GetLivePriceUseCase getLivePriceUseCase;
    private final FindSnapshotsUseCase findSnapshotsUseCase;
    private final Clock clock;

    @Override
    public Optional<RegretReport> generateReport(GenerateRegretReportCommand command) {
        List<ViolatedOrder> violations = findViolations(command);
        if (violations.isEmpty()) {
            return Optional.empty();
        }

        List<ViolationDetail> details = calculateViolationDetails(violations);
        AssetSnapshot snapshot = getLatestSnapshot(command);
        List<RuleImpact> impacts = new ViolationDetails(details).toRuleImpacts(snapshot.getTotalInvestment());

        return Optional.of(RegretReport.generate(
            command.userId(), command.roundId(), command.exchangeId(),
            snapshot.getTotalProfitRate(), snapshot.getTotalInvestment(),
            impacts, details,
            command.startedAt().toLocalDate(), LocalDate.now(clock),
            LocalDateTime.now(clock)
        ));
    }

    private List<ViolatedOrder> findViolations(GenerateRegretReportCommand command) {
        FindViolatedOrdersQuery query = new FindViolatedOrdersQuery(
            command.roundId(), command.exchangeId(), command.walletId());

        return findViolatedOrdersUseCase.findViolatedOrders(query).stream()
            .map(this::toViolatedOrder)
            .toList();
    }

    private ViolatedOrder toViolatedOrder(ViolatedOrderResult result) {
        List<SoldPortion> soldPortions = result.soldPortions().stream()
            .map(sp -> new SoldPortion(sp.filledPrice(), sp.quantity()))
            .toList();

        return ViolatedOrder.create(
            result.orderId(), result.ruleId(), result.ruleType(),
            TradeSide.valueOf(result.side()), result.filledPrice(),
            result.quantity(), result.amount(),
            result.exchangeCoinId(), result.violatedAt(),
            soldPortions);
    }

    private List<ViolationDetail> calculateViolationDetails(List<ViolatedOrder> violations) {
        CurrentPrices currentPrices = resolveCurrentPrices(violations);
        return violations.stream()
            .map(v -> {
                BigDecimal lossAmount = v.calculateLoss(currentPrices.getPrice(v.getExchangeCoinId()));
                return ViolationDetail.create(
                    v.getOrderId(), v.getRuleId(), v.getExchangeCoinId(),
                    lossAmount, lossAmount, v.getViolatedAt());
            })
            .toList();
    }

    private CurrentPrices resolveCurrentPrices(List<ViolatedOrder> violations) {
        Map<Long, BigDecimal> priceMap = violations.stream()
            .map(ViolatedOrder::getExchangeCoinId)
            .distinct()
            .collect(Collectors.toMap(id -> id, getLivePriceUseCase::getCurrentPrice));
        return new CurrentPrices(priceMap);
    }

    private AssetSnapshot getLatestSnapshot(GenerateRegretReportCommand command) {
        return findSnapshotsUseCase.findLatestByRoundIdAndExchangeId(
                command.roundId(), command.exchangeId())
            .map(this::toAssetSnapshot)
            .orElseThrow(() -> new CustomException(ErrorCode.SNAPSHOT_NOT_FOUND));
    }

    private AssetSnapshot toAssetSnapshot(SnapshotInfoResult result) {
        return AssetSnapshot.reconstitute(
            result.snapshotId(), result.roundId(), result.exchangeId(),
            result.totalAsset(), result.totalInvestment(),
            result.totalProfitRate(), result.snapshotDate());
    }
}
