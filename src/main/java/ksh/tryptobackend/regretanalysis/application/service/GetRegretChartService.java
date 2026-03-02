package ksh.tryptobackend.regretanalysis.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.regretanalysis.application.port.in.GetRegretChartUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretChartQuery;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretChartResult;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretChartResult.DailyComparison;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretChartResult.ViolationMarkerPoint;
import ksh.tryptobackend.regretanalysis.application.port.out.BtcPriceHistoryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.ExchangeInfoPort;
import ksh.tryptobackend.regretanalysis.application.port.out.InvestmentRoundPort;
import ksh.tryptobackend.regretanalysis.application.port.out.PortfolioSnapshotPort;
import ksh.tryptobackend.regretanalysis.application.port.out.RegretReportPersistencePort;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.BtcDailyPrice;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.ExchangeInfoRecord;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.RoundInfoResult;
import ksh.tryptobackend.regretanalysis.domain.model.AssetSnapshot;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import ksh.tryptobackend.regretanalysis.domain.vo.AssetTimeline;
import ksh.tryptobackend.regretanalysis.domain.vo.BtcBenchmark;
import ksh.tryptobackend.regretanalysis.domain.vo.CumulativeLossTimeline;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationMarkers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetRegretChartService implements GetRegretChartUseCase {

    private final InvestmentRoundPort investmentRoundPort;
    private final RegretReportPersistencePort regretReportPersistencePort;
    private final PortfolioSnapshotPort portfolioSnapshotPort;
    private final BtcPriceHistoryPort btcPriceHistoryPort;
    private final ExchangeInfoPort exchangeInfoPort;

    @Override
    @Transactional(readOnly = true)
    public RegretChartResult getRegretChart(GetRegretChartQuery query) {
        getRoundAndValidateOwner(query);
        validateReportExists(query);
        List<ViolationDetail> violations = getViolationDetails(query);
        ExchangeInfoRecord exchangeInfo = getExchangeInfo(query.exchangeId());
        AssetTimeline timeline = getAssetTimeline(query);

        CumulativeLossTimeline lossTimeline = CumulativeLossTimeline.build(violations, timeline.getDates());
        BtcBenchmark btcBenchmark = buildBtcBenchmark(timeline, exchangeInfo.currency());
        ViolationMarkers violationMarkers = ViolationMarkers.from(violations, timeline);

        List<DailyComparison> assetHistory = mapToAssetHistory(timeline, lossTimeline, btcBenchmark);
        List<ViolationMarkerPoint> markerPoints = mapToViolationMarkerPoints(violationMarkers);

        return new RegretChartResult(
            query.roundId(), query.exchangeId(),
            exchangeInfo.name(), exchangeInfo.currency(),
            timeline.calculateTotalDays(), assetHistory, markerPoints
        );
    }

    private void getRoundAndValidateOwner(GetRegretChartQuery query) {
        RoundInfoResult round = investmentRoundPort.getRound(query.roundId());
        if (!round.userId().equals(query.userId())) {
            throw new CustomException(ErrorCode.ROUND_ACCESS_DENIED);
        }
    }

    private void validateReportExists(GetRegretChartQuery query) {
        if (!regretReportPersistencePort.existsByRoundIdAndExchangeId(query.roundId(), query.exchangeId())) {
            throw new CustomException(ErrorCode.REPORT_NOT_FOUND);
        }
    }

    private List<ViolationDetail> getViolationDetails(GetRegretChartQuery query) {
        return regretReportPersistencePort.findViolationDetailsByRoundIdAndExchangeId(
            query.roundId(), query.exchangeId());
    }

    private ExchangeInfoRecord getExchangeInfo(Long exchangeId) {
        return exchangeInfoPort.getExchangeInfo(exchangeId);
    }

    private AssetTimeline getAssetTimeline(GetRegretChartQuery query) {
        List<AssetSnapshot> snapshots = portfolioSnapshotPort.findAllByRoundIdAndExchangeId(
            query.roundId(), query.exchangeId());
        return AssetTimeline.of(snapshots);
    }

    private BtcBenchmark buildBtcBenchmark(AssetTimeline timeline, String currency) {
        List<BtcDailyPrice> btcPrices = btcPriceHistoryPort.findBtcDailyPrices(
            timeline.getStartDate(), timeline.getEndDate(), currency);
        Map<LocalDate, BigDecimal> priceMap = btcPrices.stream()
            .collect(Collectors.toMap(BtcDailyPrice::date, BtcDailyPrice::closePrice));

        return BtcBenchmark.calculate(timeline.getSeedMoney(), priceMap, timeline.getDates(), timeline.getStartDate());
    }

    private List<DailyComparison> mapToAssetHistory(AssetTimeline timeline,
                                                    CumulativeLossTimeline lossTimeline,
                                                    BtcBenchmark btcBenchmark) {
        return timeline.getSnapshots().stream()
            .map(snapshot -> {
                LocalDate date = snapshot.getSnapshotLocalDate();
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
