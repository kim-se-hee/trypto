package ksh.tryptobackend.regretanalysis.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchange;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRule;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRules;
import ksh.tryptobackend.regretanalysis.domain.vo.ExchangeCatalog;
import ksh.tryptobackend.regretanalysis.domain.vo.RealizedLoss;
import ksh.tryptobackend.regretanalysis.domain.vo.RoundRuleImpact;
import ksh.tryptobackend.regretanalysis.domain.vo.RuleFollowedAssetTimeline;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolatedRule;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLoss;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLossBreakdown;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationRow;

/** 한 라운드에 속한 거래소별 복기 리포트의 묶음. 배치는 거래소마다 기축통화로 리포트를 남기므로, 라운드 하나로 보여주려면 이 자리에서 원화로 환산해 합친다. */
public class RegretReports {

    private final List<RegretReport> reports;

    private RegretReports(List<RegretReport> reports) {
        this.reports = List.copyOf(reports);
    }

    public static RegretReports of(List<RegretReport> reports) {
        return new RegretReports(reports);
    }

    public Set<Long> extractExchangeIds() {
        return reports.stream().map(RegretReport::getExchangeId).collect(Collectors.toSet());
    }

    public Set<Long> extractCoinIds() {
        return reports.stream()
                .flatMap(report -> report.getViolationDetails().extractCoinIds().stream())
                .collect(Collectors.toSet());
    }

    /** 그래프의 원칙 준수 곡선과 위반 마커는 발생일과 원화 손실만 있으면 그릴 수 있다. 손실은 실현 여부에 따라 반영 방법이 달라 나뉜 채로 넘긴다. */
    public List<ViolationLoss> toViolationLosses(ExchangeCatalog exchanges) {
        return reports.stream()
                .flatMap(report -> report.getViolationDetails().toList().stream()
                        .map(detail -> new ViolationLoss(
                                detail.getOccurredDate(), toKrwLoss(exchanges.get(report.getExchangeId()), detail))))
                .sorted(Comparator.comparing(ViolationLoss::occurredDate))
                .toList();
    }

    public RoundRegretReport merge(
            Long roundId,
            AnalysisRules rules,
            ExchangeCatalog exchanges,
            RuleFollowedAssetTimeline ruleFollowedAssets) {
        BigDecimal actualAsset = sumActualAsset(exchanges);

        return RoundRegretReport.of(
                roundId,
                sumTotalViolations(),
                sumTotalViolationLoss(exchanges),
                actualAsset,
                ruleFollowedAssets.calculateFinalAsset(actualAsset),
                findAnalysisStart(),
                findAnalysisEnd(),
                mergeRuleImpacts(rules, exchanges),
                mergeViolationRows(rules, exchanges));
    }

    private static ViolationLossBreakdown toKrwLoss(AnalysisExchange exchange, ViolationDetail detail) {
        return new ViolationLossBreakdown(
                exchange.toKrw(detail.getUnrealizedLossAmount()), toKrwRealizedLosses(exchange, List.of(detail)));
    }

    private static List<RealizedLoss> toKrwRealizedLosses(AnalysisExchange exchange, List<ViolationDetail> details) {
        return details.stream()
                .flatMap(detail -> detail.getRealizedLosses().stream())
                .map(realized -> new RealizedLoss(realized.realizedOn(), exchange.toKrw(realized.amount())))
                .toList();
    }

    private int sumTotalViolations() {
        return reports.stream().mapToInt(RegretReport::getTotalViolations).sum();
    }

    private BigDecimal sumTotalViolationLoss(ExchangeCatalog exchanges) {
        return reports.stream()
                .map(report -> exchanges.toKrw(report.getExchangeId(), report.getTotalViolationLoss()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumActualAsset(ExchangeCatalog exchanges) {
        return reports.stream()
                .map(report -> exchanges.toKrw(report.getExchangeId(), report.getActualAsset()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDate findAnalysisStart() {
        return reports.stream()
                .map(RegretReport::getAnalysisStart)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    private LocalDate findAnalysisEnd() {
        return reports.stream()
                .map(RegretReport::getAnalysisEnd)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    private List<RoundRuleImpact> mergeRuleImpacts(AnalysisRules rules, ExchangeCatalog exchanges) {
        Map<Long, RoundRuleImpact> mergedByRuleId = new LinkedHashMap<>();
        for (RegretReport report : reports) {
            for (RuleImpact impact : report.getRuleImpacts()) {
                rules.findById(impact.getRuleId())
                        .ifPresent(rule -> mergedByRuleId.merge(
                                rule.ruleId(),
                                toRoundRuleImpact(rule, impact, report, exchanges),
                                RegretReports::accumulate));
            }
        }

        return rules.rules().stream()
                .sorted(Comparator.comparing(AnalysisRule::ruleId))
                .map(rule -> mergedByRuleId.getOrDefault(rule.ruleId(), RoundRuleImpact.notViolated(rule)))
                .toList();
    }

    private RoundRuleImpact toRoundRuleImpact(
            AnalysisRule rule, RuleImpact impact, RegretReport report, ExchangeCatalog exchanges) {
        return new RoundRuleImpact(
                rule, impact.getViolationCount(), exchanges.toKrw(report.getExchangeId(), impact.getTotalLossAmount()));
    }

    private static RoundRuleImpact accumulate(RoundRuleImpact merged, RoundRuleImpact added) {
        return new RoundRuleImpact(
                merged.rule(),
                merged.violationCount() + added.violationCount(),
                merged.totalLossAmountKrw().add(added.totalLossAmountKrw()));
    }

    private List<ViolationRow> mergeViolationRows(AnalysisRules rules, ExchangeCatalog exchanges) {
        List<ViolationRow> rows = new ArrayList<>();
        for (RegretReport report : reports) {
            AnalysisExchange exchange = exchanges.get(report.getExchangeId());
            report.getViolationDetails()
                    .groupByOrder()
                    .forEach((orderId, details) -> rows.add(toOrderRow(exchange, details, rules)));
            report.getViolationDetails()
                    .findMonitoringViolations()
                    .forEach(detail -> rows.add(toMonitoringRow(exchange, detail, rules)));
        }
        return rows.stream()
                .sorted(Comparator.comparing(ViolationRow::occurredAt))
                .toList();
    }

    private ViolationRow toOrderRow(AnalysisExchange exchange, List<ViolationDetail> details, AnalysisRules rules) {
        ViolationDetail first = details.getFirst();
        return new ViolationRow(
                first.getViolationDetailId(),
                first.getOrderId(),
                exchange,
                first.getCoinId(),
                toViolatedRules(exchange, details, rules),
                first.getOccurredAt());
    }

    private ViolationRow toMonitoringRow(AnalysisExchange exchange, ViolationDetail detail, AnalysisRules rules) {
        return new ViolationRow(
                detail.getViolationDetailId(),
                null,
                exchange,
                detail.getCoinId(),
                toViolatedRules(exchange, List.of(detail), rules),
                detail.getOccurredAt());
    }

    /** 한 주문이 같은 원칙을 여러 번 어겼다면 위반 손실을 합쳐 하나로 만든다. 그래프가 원칙별로 곡선을 되돌릴 때 이 금액을 사용한다. */
    private List<ViolatedRule> toViolatedRules(
            AnalysisExchange exchange, List<ViolationDetail> details, AnalysisRules rules) {
        Map<RuleType, List<ViolationDetail>> detailsByRuleType = new LinkedHashMap<>();
        for (ViolationDetail detail : details) {
            rules.findById(detail.getRuleId())
                    .ifPresent(rule -> detailsByRuleType
                            .computeIfAbsent(rule.ruleType(), ruleType -> new ArrayList<>())
                            .add(detail));
        }

        return detailsByRuleType.entrySet().stream()
                .map(entry -> toViolatedRule(exchange, entry.getKey(), entry.getValue()))
                .toList();
    }

    private ViolatedRule toViolatedRule(AnalysisExchange exchange, RuleType ruleType, List<ViolationDetail> details) {
        BigDecimal lossAmount = sumOf(details, ViolationDetail::getLossAmount);
        BigDecimal unrealizedLossKrw = exchange.toKrw(sumOf(details, ViolationDetail::getUnrealizedLossAmount));

        return new ViolatedRule(
                ruleType,
                lossAmount,
                new ViolationLossBreakdown(unrealizedLossKrw, toKrwRealizedLosses(exchange, details)));
    }

    private BigDecimal sumOf(List<ViolationDetail> details, Function<ViolationDetail, BigDecimal> amount) {
        return details.stream().map(amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
