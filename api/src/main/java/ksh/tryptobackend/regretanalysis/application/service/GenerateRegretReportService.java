package ksh.tryptobackend.regretanalysis.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.GetLivePriceUseCase;
import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotInfoResult;
import ksh.tryptobackend.regretanalysis.application.port.in.GenerateRegretReportUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.command.GenerateRegretReportCommand;
import ksh.tryptobackend.regretanalysis.domain.model.AssetSnapshot;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.RuleImpact;
import ksh.tryptobackend.regretanalysis.domain.model.ViolatedOrder;
import ksh.tryptobackend.regretanalysis.domain.model.ViolatedOrders;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetails;
import ksh.tryptobackend.regretanalysis.domain.vo.CurrentPrices;
import ksh.tryptobackend.regretanalysis.domain.vo.TradeSide;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLossContext.SoldPortion;
import ksh.tryptobackend.trading.application.port.in.FindViolatedOrdersUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.query.FindViolatedOrdersQuery;
import ksh.tryptobackend.trading.application.port.in.dto.result.ViolatedOrderResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenerateRegretReportService implements GenerateRegretReportUseCase {

    private final FindViolatedOrdersUseCase findViolatedOrdersUseCase;
    private final GetLivePriceUseCase getLivePriceUseCase;
    private final FindSnapshotsUseCase findSnapshotsUseCase;
    private final Clock clock;

    @Override
    public Optional<RegretReport> generateReport(GenerateRegretReportCommand command) {
        ViolatedOrders violations = findViolations(command);
        if (violations.isEmpty()) {
            return Optional.empty();
        }

        CurrentPrices currentPrices = resolveCurrentPrices(violations.exchangeCoinIds());
        List<ViolationDetail> details = violations.calculateDetails(currentPrices);
        AssetSnapshot snapshot = getLatestSnapshot(command);
        List<RuleImpact> impacts =
                new ViolationDetails(details).toRuleImpacts(snapshot.getTotalInvestment());

        return Optional.of(
                RegretReport.generate(
                        command.userId(),
                        command.roundId(),
                        command.exchangeId(),
                        snapshot.getTotalProfitRate(),
                        snapshot.getTotalInvestment(),
                        impacts,
                        details,
                        command.startedAt().toLocalDate(),
                        LocalDate.now(clock),
                        LocalDateTime.now(clock)));
    }

    private ViolatedOrders findViolations(GenerateRegretReportCommand command) {
        FindViolatedOrdersQuery query =
                new FindViolatedOrdersQuery(
                        command.roundId(), command.exchangeId(), command.walletId());

        List<ViolatedOrder> orders =
                findViolatedOrdersUseCase.findViolatedOrders(query).stream()
                        .map(this::toViolatedOrder)
                        .toList();
        return new ViolatedOrders(orders);
    }

    private ViolatedOrder toViolatedOrder(ViolatedOrderResult result) {
        List<SoldPortion> soldPortions =
                result.soldPortions().stream()
                        .map(sp -> new SoldPortion(sp.filledPrice(), sp.quantity()))
                        .toList();

        return ViolatedOrder.create(
                result.orderId(),
                result.ruleId(),
                result.ruleType(),
                TradeSide.valueOf(result.side()),
                result.filledPrice(),
                result.quantity(),
                result.amount(),
                result.exchangeCoinId(),
                result.violatedAt(),
                soldPortions);
    }

    private CurrentPrices resolveCurrentPrices(java.util.Set<Long> exchangeCoinIds) {
        Map<Long, BigDecimal> priceMap =
                exchangeCoinIds.stream()
                        .collect(Collectors.toMap(id -> id, getLivePriceUseCase::getCurrentPrice));
        return new CurrentPrices(priceMap);
    }

    private AssetSnapshot getLatestSnapshot(GenerateRegretReportCommand command) {
        return findSnapshotsUseCase
                .findLatestByRoundIdAndExchangeId(command.roundId(), command.exchangeId())
                .map(this::toAssetSnapshot)
                .orElseThrow(() -> new CustomException(ErrorCode.SNAPSHOT_NOT_FOUND));
    }

    private AssetSnapshot toAssetSnapshot(SnapshotInfoResult result) {
        return AssetSnapshot.reconstitute(
                result.snapshotId(),
                result.roundId(),
                result.exchangeId(),
                result.totalAsset(),
                result.totalInvestment(),
                result.totalProfitRate(),
                result.snapshotDate());
    }
}
