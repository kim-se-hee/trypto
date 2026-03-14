package ksh.tryptobackend.regretanalysis.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.FindRoundInfoUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RoundInfoResult;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotInfoResult;
import ksh.tryptobackend.regretanalysis.application.port.in.GetRegretChartUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretChartQuery;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretChartResult;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretChartResult.DailyComparison;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretChartResult.ViolationMarkerPoint;
import ksh.tryptobackend.regretanalysis.application.port.out.BtcPriceHistoryQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.RegretReportQueryPort;
import ksh.tryptobackend.regretanalysis.domain.model.AssetSnapshot;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchange;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRound;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRoundStatus;
import ksh.tryptobackend.regretanalysis.domain.vo.AssetTimeline;
import ksh.tryptobackend.regretanalysis.domain.vo.BtcBenchmark;
import ksh.tryptobackend.regretanalysis.domain.vo.BtcDailyPrice;
import ksh.tryptobackend.regretanalysis.domain.vo.BtcDailyPrices;
import ksh.tryptobackend.regretanalysis.domain.vo.CumulativeLossTimeline;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationMarkers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetRegretChartService implements GetRegretChartUseCase {

    private final FindRoundInfoUseCase findRoundInfoUseCase;
    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final FindSnapshotsUseCase findSnapshotsUseCase;
    private final RegretReportQueryPort regretReportQueryPort;
    private final BtcPriceHistoryQueryPort btcPriceHistoryQueryPort;

    @Override
    @Transactional(readOnly = true)
    public RegretChartResult getRegretChart(GetRegretChartQuery query) {
        getRoundAndValidateOwner(query);
        validateReportExists(query);
        List<ViolationDetail> violations = getViolationDetails(query);
        AnalysisExchange exchange = getExchangeInfo(query.exchangeId());
        AssetTimeline timeline = getAssetTimeline(query);

        CumulativeLossTimeline lossTimeline = CumulativeLossTimeline.build(violations, timeline.getDates());
        BtcBenchmark btcBenchmark = buildBtcBenchmark(timeline, exchange.currency());
        ViolationMarkers violationMarkers = ViolationMarkers.from(violations, timeline);

        List<DailyComparison> assetHistory = mapToAssetHistory(timeline, lossTimeline, btcBenchmark);
        List<ViolationMarkerPoint> markerPoints = mapToViolationMarkerPoints(violationMarkers);

        return new RegretChartResult(
            query.roundId(), query.exchangeId(),
            exchange.name(), exchange.currency(),
            timeline.calculateTotalDays(), assetHistory, markerPoints
        );
    }

    private void getRoundAndValidateOwner(GetRegretChartQuery query) {
        AnalysisRound round = getRound(query.roundId());
        if (!round.userId().equals(query.userId())) {
            throw new CustomException(ErrorCode.ROUND_ACCESS_DENIED);
        }
    }

    private AnalysisRound getRound(Long roundId) {
        RoundInfoResult result = findRoundInfoUseCase.findById(roundId)
            .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_FOUND));
        return toAnalysisRound(result);
    }

    private AnalysisRound toAnalysisRound(RoundInfoResult result) {
        return new AnalysisRound(
            result.roundId(), result.userId(), result.initialSeed(),
            AnalysisRoundStatus.valueOf(result.status()),
            result.startedAt(), result.endedAt());
    }

    private void validateReportExists(GetRegretChartQuery query) {
        if (!regretReportQueryPort.existsByRoundIdAndExchangeId(query.roundId(), query.exchangeId())) {
            throw new CustomException(ErrorCode.REPORT_NOT_FOUND);
        }
    }

    private List<ViolationDetail> getViolationDetails(GetRegretChartQuery query) {
        return regretReportQueryPort.findViolationDetailsByRoundIdAndExchangeId(
            query.roundId(), query.exchangeId());
    }

    private AnalysisExchange getExchangeInfo(Long exchangeId) {
        ExchangeDetailResult result = findExchangeDetailUseCase.findExchangeDetail(exchangeId)
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
        String currency = result.domestic() ? "KRW" : "USD";
        return new AnalysisExchange(exchangeId, result.name(), currency);
    }

    private AssetTimeline getAssetTimeline(GetRegretChartQuery query) {
        List<AssetSnapshot> snapshots = findSnapshotsUseCase.findAllByRoundIdAndExchangeId(
                query.roundId(), query.exchangeId()).stream()
            .map(this::toAssetSnapshot)
            .toList();
        return AssetTimeline.of(snapshots);
    }

    private AssetSnapshot toAssetSnapshot(SnapshotInfoResult result) {
        return AssetSnapshot.reconstitute(
            result.snapshotId(), result.roundId(), result.exchangeId(),
            result.totalAsset(), result.totalInvestment(),
            result.totalProfitRate(), result.snapshotDate());
    }

    private BtcBenchmark buildBtcBenchmark(AssetTimeline timeline, String currency) {
        List<BtcDailyPrice> btcPrices = btcPriceHistoryQueryPort.findBtcDailyPrices(
            timeline.getStartDate(), timeline.getEndDate(), currency);
        BtcDailyPrices dailyPrices = BtcDailyPrices.of(btcPrices);

        return BtcBenchmark.calculate(timeline.getSeedMoney(), dailyPrices.toMap(), timeline.getDates(), timeline.getStartDate());
    }

    private List<DailyComparison> mapToAssetHistory(AssetTimeline timeline,
                                                    CumulativeLossTimeline lossTimeline,
                                                    BtcBenchmark btcBenchmark) {
        return timeline.getSnapshots().stream()
            .map(snapshot -> {
                LocalDate date = snapshot.getSnapshotDate();
                BigDecimal actualAsset = snapshot.getTotalAsset();
                return new DailyComparison(
                    date,
                    actualAsset,
                    lossTimeline.calculateRuleFollowedAsset(actualAsset, date),
                    btcBenchmark.getAssetValueAt(date)
                );
            })
            .toList();
    }

    private List<ViolationMarkerPoint> mapToViolationMarkerPoints(ViolationMarkers violationMarkers) {
        return violationMarkers.getMarkers().stream()
            .map(marker -> new ViolationMarkerPoint(marker.date(), marker.assetValue()))
            .toList();
    }
}
